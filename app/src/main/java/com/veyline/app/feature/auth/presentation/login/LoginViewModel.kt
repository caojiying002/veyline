package com.veyline.app.feature.auth.presentation.login

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veyline.app.data.network.result.ApiResult
import com.veyline.app.feature.auth.data.AuthRepository
import com.veyline.app.ui.error.UiError
import com.veyline.app.ui.error.toUiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 账号被限制登录的业务错误码，该码对应的服务端提示文案已确认可以直接展示 */
private const val CODE_LOGIN_RESTRICTED = -2

data class LoginUiState(
    val isSubmitting: Boolean = false,
    val nameError: String? = null,
    val passwordError: String? = null,
)

sealed interface LoginAction {

    data class Submit(
        val name: String,
        val password: String,
    ) : LoginAction

    data object ClearNameError : LoginAction

    data object ClearPasswordError : LoginAction
}

sealed interface LoginEffect {

    data object LoginSucceeded : LoginEffect

    data class ShowGeneralError(
        val error: UiError,
    ) : LoginEffect
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effect = Channel<LoginEffect>()
    val effect = _effect.receiveAsFlow()

    private var loginJob: Job? = null

    @MainThread
    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.Submit -> submit(action.name, action.password)
            LoginAction.ClearNameError -> clearNameError()
            LoginAction.ClearPasswordError -> clearPasswordError()
        }
    }

    private fun submit(name: String, password: String) {
        if (loginJob?.isActive == true) {
            return
        }

        // 用户名两侧多余的空格大多是误触或复制粘贴导致的，trim 掉不影响语义。
        // 密码不做任何清洗：空格可能是密码本身有意义的一部分，静默去掉会导致“明明输对了却登录失败”且无法自查。
        val trimmedName = name.trim()

        loginJob = viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, nameError = null, passwordError = null) }

            val result = authRepository.login(trimmedName, password)

            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effect.send(LoginEffect.LoginSucceeded)
                }

                is ApiResult.Failure.Validation -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            nameError = result.fieldErrors[FIELD_NAME],
                            passwordError = result.fieldErrors[FIELD_PASSWORD],
                        )
                    }
                }

                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effect.send(LoginEffect.ShowGeneralError(result.toLoginUiError()))
                }
            }
        }
    }

    private fun clearNameError() {
        _uiState.update { it.copy(nameError = null) }
    }

    private fun clearPasswordError() {
        _uiState.update { it.copy(passwordError = null) }
    }

    private companion object {
        const val FIELD_NAME = "name"

        const val FIELD_PASSWORD = "password"
    }
}

/**
 * 将登录失败转换为 UI 错误，对已确认安全展示的业务错误单独处理
 *
 * 只有 [CODE_LOGIN_RESTRICTED] 对应的业务错误经过确认可以直接展示服务端文案，
 * 其余失败一律交给 [toUiError] 统一降级，避免把未经确认的服务端消息（可能是英文或
 * 内部调试信息）直接展示给用户
 */
private fun ApiResult.Failure.toLoginUiError(): UiError =
    if (
        this is ApiResult.Failure.Business &&
        code == CODE_LOGIN_RESTRICTED &&
        message.isNotBlank()
    ) {
        UiError.DisplayReady(message)
    } else {
        toUiError()
    }
