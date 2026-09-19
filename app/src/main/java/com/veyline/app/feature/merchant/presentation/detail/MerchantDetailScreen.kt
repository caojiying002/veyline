package com.veyline.app.feature.merchant.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veyline.app.R
import com.veyline.app.feature.merchant.domain.model.MerchantDetail
import com.veyline.app.ui.components.AppBackTopBar
import com.veyline.app.ui.components.AppCard
import com.veyline.app.ui.components.AppErrorContent
import com.veyline.app.ui.components.AppLoadingContent
import com.veyline.app.ui.components.DetailImageGrid
import com.veyline.app.ui.error.UiError
import com.veyline.app.ui.theme.CardContentPadding
import com.veyline.app.ui.theme.DefaultHorizontalSpace
import com.veyline.app.ui.theme.DefaultVerticalSpace
import com.veyline.app.ui.theme.DividerHeight
import com.veyline.app.ui.theme.SpacingLarge
import com.veyline.app.ui.theme.SpacingSmall
import com.veyline.app.ui.theme.VeylineTextStyles
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
                        val merchant = checkNotNull(uiState.merchant)
                        MerchantDetailContent(
                            merchant = merchant,
                            onImageClick = {},
                            contentPadding = bottomInsets.asPaddingValues(),
                            modifier = Modifier.fillMaxSize(),
                        )
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

@Composable
private fun MerchantDetailContent(
    merchant: MerchantDetail,
    onImageClick: (Int) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            // 两个 padding 都必须放在 verticalScroll 后面，成为可滚动内容的一部分，
            // 而不是缩小滚动视口
            .padding(contentPadding)
            .padding(
                horizontal = DefaultHorizontalSpace,
                vertical = DividerHeight,
            ),
        verticalArrangement = Arrangement.spacedBy(DividerHeight)
    ) {
        DetailImageGrid(
            imageUrls = merchant.imageUrls,
            onImageClick = onImageClick,
            modifier = Modifier.fillMaxWidth()
        )

        MerchantBasicInfoCard(
            merchant = merchant,
            modifier = Modifier.fillMaxWidth()
        )

        // 后续接入登录状态和联系方式
    }
}

@Composable
private fun MerchantBasicInfoCard(
    merchant: MerchantDetail,
    modifier: Modifier = Modifier,
) {
    // 地区图标跟随字体缩放
    val iconSize = with(LocalDensity.current) { 16.sp.toDp() }

    AppCard(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = CardContentPadding,
            vertical = DefaultVerticalSpace
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SpacingLarge),
        ) {
            Text(
                text = merchant.name,
                style = VeylineTextStyles.Title,
                color = VeylineTheme.colors.textTitle,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_location),
                    contentDescription = null,
                    tint = VeylineTheme.colors.primary,
                    modifier = Modifier.size(iconSize),
                )
                Spacer(modifier = Modifier.width(SpacingSmall))
                Text(
                    text = merchant.provinceCode, // TODO 转换为省份名显示，同 MerchantListItem
                    style = VeylineTextStyles.Body,
                    color = VeylineTheme.colors.primary,
                )
            }

            Text(
                text = merchant.description,
                style = VeylineTextStyles.Body,
                color = VeylineTheme.colors.textContent,
            )
        }
    }
}
