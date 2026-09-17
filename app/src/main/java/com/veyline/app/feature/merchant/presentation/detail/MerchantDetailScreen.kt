package com.veyline.app.feature.merchant.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veyline.app.R
import com.veyline.app.ui.components.AppBackTopBar
import com.veyline.app.ui.components.AppErrorContent
import com.veyline.app.ui.components.AppLoadingContent
import com.veyline.app.ui.error.UiError
import com.veyline.app.ui.theme.VeylineTheme

private const val VIEW_MODEL_KEY_PREFIX = "merchant:detail:"

@Composable
fun MerchantDetailRoute(
    merchantId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MerchantDetailViewModel =
        hiltViewModel<MerchantDetailViewModel, MerchantDetailViewModel.Factory>(
            // key 带上 merchantId，避免以后 entry 被复用（如 launchSingleTop）时
            // 因为 key 不变而拿到上一个商家缓存的 ViewModel
            key = "$VIEW_MODEL_KEY_PREFIX$merchantId",
            creationCallback = { factory -> factory.create(merchantId) },
        )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.onAction(MerchantDetailAction.InitialLoad)
    }

    MerchantDetailScreen(
        uiState = uiState,
        onBackClick = onNavigateBack,
        onRetryClick = { viewModel.onAction(MerchantDetailAction.Retry) },
        onRefresh = { viewModel.onAction(MerchantDetailAction.Refresh) },
        modifier = modifier,
    )
}

@Composable
fun MerchantDetailScreen(
    uiState: MerchantDetailUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VeylineTheme.colors.background),
    ) {
        AppBackTopBar(
            title = stringResource(R.string.action_back),
            onBackClick = onBackClick,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val bottomInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
            val uiError: UiError? = uiState.error

            when {
                uiState.hasContent -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // 下一步在这里实现商家详情内容
                    }
                }

                uiState.isLoading -> {
                    AppLoadingContent(
                        modifier = Modifier.windowInsetsPadding(bottomInsets),
                    )
                }

                uiError != null -> {
                    val message = when (uiError) {
                        UiError.Connection -> stringResource(R.string.error_connection)
                        UiError.Technical -> stringResource(R.string.error_technical)
                        is UiError.DisplayReady -> uiError.message
                    }

                    AppErrorContent(
                        message = message,
                        onRetryClick = onRetryClick,
                        modifier = Modifier.windowInsetsPadding(bottomInsets),
                    )
                }

                else -> Unit
            }
        }
    }
}
