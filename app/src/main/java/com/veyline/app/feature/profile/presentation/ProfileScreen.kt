package com.veyline.app.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.veyline.app.R
import com.veyline.app.ui.theme.SpacingLarge
import com.veyline.app.ui.theme.SpacingMedium
import com.veyline.app.ui.theme.ToolbarHeight
import com.veyline.app.ui.theme.VeylineTextStyles
import com.veyline.app.ui.theme.VeylineTheme

private const val VIEW_MODEL_KEY = "profile"

@Composable
fun ProfileRoute(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(
        key = VIEW_MODEL_KEY,
    ),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                // TODO
            }
        }
    }

    ProfileScreen(
        uiState = uiState,
        onLoginClick = onNavigateToLogin,
        onRegisterClick = onNavigateToRegister,
        onRefreshClick = { viewModel.onAction(ProfileAction.Refresh) },
        onRetryClick = { viewModel.onAction(ProfileAction.Retry) },
        onLogoutClick = {
            // TODO 需要补上确认Dialog，目前快速跑通业务逻辑省略了Dialog
            viewModel.onAction(ProfileAction.Logout)
        },
        modifier = modifier,
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onRetryClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VeylineTheme.colors.background)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
            ),
    ) {
        ProfileTopBar()

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // 未登录卡片或已登录资料区域
            ProfileAccountSection(
                accountState = uiState.accountState,
                onLoginClick = onLoginClick,
                onRegisterClick = onRegisterClick,
            )

            // 功能菜单
        }
    }
}

@Composable
private fun ProfileTopBar(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ToolbarHeight)
            .padding(
                horizontal = SpacingLarge,
                vertical = SpacingMedium,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = VeylineTextStyles.Title,
            color = VeylineTheme.colors.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
    }
}

@Composable
private fun ProfileAccountSection(
    accountState: ProfileAccountState,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (accountState) {
        ProfileAccountState.Initializing -> {
            // 读取DataStore时什么都不画，但大概率会让ProfileAccountSection下方的功能菜单在加载成功时出现跳跃，待优化
            // 可能用一个AppCard + heightIn(min)实现？
        }

        ProfileAccountState.LoggedOut ->
            ProfileLoggedOutCard(
                onLoginClick = onLoginClick,
                onRegisterClick = onRegisterClick,
                modifier = modifier,
            )

        is ProfileAccountState.LoggedIn -> {
            // 占位实现，这一步先不做
            ProfileLoggedInCard(
                loadState = accountState.loadState,
                modifier = modifier,
            )
        }
    }
}

/** 未登录时的个人中心卡片，引导用户登录或注册 */
@Composable
private fun ProfileLoggedOutCard(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

}

@Composable
private fun ProfileLoggedInCard(
    loadState: ProfileLoadState,
    modifier: Modifier = Modifier,
) {
    when (loadState) {
        ProfileLoadState.Loading -> Unit  // ProfileLoadingContent 或不抽取
        is ProfileLoadState.Error -> Unit //
        is ProfileLoadState.Success -> Unit
    }
}
