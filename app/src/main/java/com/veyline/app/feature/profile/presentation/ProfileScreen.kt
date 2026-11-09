package com.veyline.app.feature.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle

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

}
