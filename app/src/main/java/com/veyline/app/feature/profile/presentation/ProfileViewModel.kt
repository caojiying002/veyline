package com.veyline.app.feature.profile.presentation

import androidx.lifecycle.ViewModel
import com.veyline.app.feature.auth.data.AuthRepository
import com.veyline.app.feature.profile.data.ProfileRepository
import com.veyline.app.feature.profile.domain.model.UserProfile
import com.veyline.app.ui.error.UiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
}
