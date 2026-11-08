package com.veyline.app.feature.profile.presentation

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veyline.app.data.network.result.ApiResult
import com.veyline.app.feature.auth.data.AuthRepository
import com.veyline.app.feature.profile.data.ProfileRepository
import com.veyline.app.feature.profile.domain.model.UserProfile
import com.veyline.app.ui.error.UiError
import com.veyline.app.ui.error.toUiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileLoadState {

    data object Loading : ProfileLoadState

    data class Error(
        val error: UiError,
    ) : ProfileLoadState

    data class Success(
        val profile: UserProfile,
    ) : ProfileLoadState
}

sealed interface ProfileAccountState {

    data object Initializing : ProfileAccountState

    data object LoggedOut : ProfileAccountState

    data class LoggedIn(
        val loadState: ProfileLoadState,
    ) : ProfileAccountState
}

data class ProfileUiState(
    val accountState: ProfileAccountState = ProfileAccountState.Initializing,
    val isLoggingOut: Boolean = false,
)

/**
 * 个人中心页面支持的操作。
 *
 * [Refresh] 与 [Retry] 目前触发完全相同的加载逻辑，区分成两个 Action 只是为了让调用方
 * （资料卡片上的"点击刷新"、错误态的重试按钮）各自表达清晰的用户意图，不代表未来一定会分叉。
 */
sealed interface ProfileAction {

    /** 用户在资料卡片上手动触发刷新，例如升级会员后确认最新状态 */
    data object Refresh : ProfileAction

    /** 用户在错误态点击重试，重新请求资料；请求进行中时忽略 */
    data object Retry : ProfileAction

    /** 用户点击退出登录；退出进行中时忽略重复触发 */
    data object Logout : ProfileAction
}

/** 个人中心需要由 UI 一次性处理的事件 */
sealed interface ProfileEffect {

    /** 已退出登录：本地 Token 已清除 */
    data object LogoutSucceeded : ProfileEffect

    /** 展示通用错误，例如退出时本地 Token 清除失败 */
    data class ShowGeneralError(
        val error: UiError,
    ) : ProfileEffect
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ProfileEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var loadProfileJob: Job? = null

    private var logoutJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.isLoggedIn
                .distinctUntilChanged()
                .collect { isLoggedIn ->
                    onLoggedInChanged(isLoggedIn)
                }
        }
    }

    private fun onLoggedInChanged(isLoggedIn: Boolean) {
        // 登录状态一变，旧的 Profile 请求就不再有意义；
        // 登录和退出都要先取消，避免误显示上一个用户状态
        loadProfileJob?.cancel()

        if (isLoggedIn) {
            _uiState.update {
                it.copy(
                    accountState = ProfileAccountState.LoggedIn(ProfileLoadState.Loading)
                )
            }
            loadProfile()
        } else {
            _uiState.update { it.copy(accountState = ProfileAccountState.LoggedOut) }
        }
    }

    @MainThread
    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Refresh,
            ProfileAction.Retry -> loadProfile()

            ProfileAction.Logout -> logout()
        }
    }

    private fun loadProfile() {
        if (
            loadProfileJob?.isActive == true ||
            _uiState.value.isLoggingOut ||
            _uiState.value.accountState !is ProfileAccountState.LoggedIn
        ) {
            return
        }

        updateLoadState(ProfileLoadState.Loading)
        loadProfileJob = viewModelScope.launch {
            val result = profileRepository.getProfile()
            // 登录状态变化时本请求会被取消；写回前显式检查，防止上一个用户的资料写进当前状态
            ensureActive()
            when (result) {
                is ApiResult.Success -> updateLoadState(
                    ProfileLoadState.Success(result.data)
                )
                is ApiResult.Failure -> updateLoadState(
                    ProfileLoadState.Error(result.toUiError())
                )
            }
        }
    }

    private fun updateLoadState(loadState: ProfileLoadState) {
        _uiState.update { state ->
            if (state.accountState is ProfileAccountState.LoggedIn) {
                state.copy(accountState = ProfileAccountState.LoggedIn(loadState))
            } else {
                state
            }
        }
    }

    private fun logout() {
        if (
            logoutJob?.isActive == true ||
            _uiState.value.accountState !is ProfileAccountState.LoggedIn
        ) {
            return
        }

        _uiState.update { it.copy(isLoggingOut = true) }
        logoutJob = viewModelScope.launch {
            val result = try {
                authRepository.logout()
            } finally {
                // 确保成功、失败或取消，都结束退出进度
                _uiState.update { it.copy(isLoggingOut = false) }
            }

            when (result) {
                is ApiResult.Success -> _effect.send(ProfileEffect.LogoutSucceeded)
                is ApiResult.Failure -> _effect.send(
                    ProfileEffect.ShowGeneralError(result.toUiError())
                )
            }
        }
    }
}
