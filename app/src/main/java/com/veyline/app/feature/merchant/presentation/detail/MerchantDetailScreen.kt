package com.veyline.app.feature.merchant.presentation.detail

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veyline.app.R
import com.veyline.app.feature.merchant.domain.model.MerchantContactAccess
import com.veyline.app.feature.merchant.domain.model.MerchantDetail
import com.veyline.app.ui.components.AppBackTopBar
import com.veyline.app.ui.components.AppButton
import com.veyline.app.ui.components.AppCard
import com.veyline.app.ui.components.AppErrorContent
import com.veyline.app.ui.components.AppLoadingContent
import com.veyline.app.ui.components.DetailImageGrid
import com.veyline.app.ui.error.UiError
import com.veyline.app.ui.theme.DefaultHorizontalSpace
import com.veyline.app.ui.theme.DividerHeight
import com.veyline.app.ui.theme.SpacingLarge
import com.veyline.app.ui.theme.SpacingSmall
import com.veyline.app.ui.theme.VeylineTextStyles
import com.veyline.app.ui.theme.VeylineTheme

private const val VIEW_MODEL_KEY_PREFIX = "merchant:detail:"

/**
 * 商家详情页面的有状态入口。
 *
 * 负责获取 Hilt 管理的 ViewModel、以生命周期感知的方式收集页面状态，并触发首次加载。
 * 登录、升级 VIP、查看大图等跳转不在这里处理，统一转交给导航层传入的回调。
 *
 * @param merchantId 要展示的商家 ID
 * @param onNavigateBack 请求返回上一页时执行的导航操作
 * @param onNavigateToImageViewer 点击图片时执行的导航操作，参数为图片列表和点击的图片索引
 * @param onNavigateToLogin 点击登录引导按钮时执行的导航操作
 * @param onNavigateToVipUpgrade 点击升级 VIP 引导按钮时执行的导航操作
 * @param modifier 传递给无状态页面根容器的 [Modifier]
 * @param viewModel 商家详情页面的 ViewModel，默认由 Hilt 提供
 */
@Composable
fun MerchantDetailRoute(
    merchantId: String,
    onNavigateBack: () -> Unit,
    onNavigateToImageViewer: (List<String>, Int) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToVipUpgrade: () -> Unit,
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
        onImageClick = onNavigateToImageViewer,
        onLoginClick = onNavigateToLogin,
        onUpgradeVipClick = onNavigateToVipUpgrade,
        modifier = modifier,
    )
}

/**
 * 商家详情页面的无状态 UI。
 *
 * 页面根据 [uiState] 展示商家详情、全屏加载或全屏错误；刷新期间保留已有内容，
 * 通过 [PullToRefreshBox] 的指示器代替整页替换成加载态。
 *
 * 标题栏负责处理顶部状态栏 Insets，内容区域通过 [MerchantDetailContent] 的
 * contentPadding 处理底部安全区域。
 *
 * @param uiState 当前页面状态
 * @param onBackClick 点击标题栏返回区域时执行的操作
 * @param onRetryClick 在全屏错误状态下点击重试按钮时执行的操作
 * @param onRefresh 下拉刷新时执行的操作
 * @param onImageClick 点击商家图片时执行的操作，参数为图片列表和点击的图片索引
 * @param onLoginClick 点击登录引导按钮时执行的操作
 * @param onUpgradeVipClick 点击升级 VIP 引导按钮时执行的操作
 * @param modifier 应用于页面根容器的 [Modifier]
 */
@Composable
fun MerchantDetailScreen(
    uiState: MerchantDetailUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onRefresh: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onLoginClick: () -> Unit,
    onUpgradeVipClick: () -> Unit,
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
                            onImageClick = { index ->
                                onImageClick(merchant.imageUrls, index)
                            },
                            onLoginClick = onLoginClick,
                            onUpgradeVipClick = onUpgradeVipClick,
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

/**
 * 商家详情的可滚动内容区域，依次展示图片网格、基本信息卡片和联系方式卡片。
 *
 * @param merchant 商家详情领域模型
 * @param onImageClick 点击图片时执行的操作，参数为点击的图片索引
 * @param onLoginClick 点击登录引导按钮时执行的操作
 * @param onUpgradeVipClick 点击升级 VIP 引导按钮时执行的操作
 * @param contentPadding 内容区域的额外内边距，用于避让底部安全区域
 * @param modifier 应用于内容区域根容器的 [Modifier]
 */
@Composable
private fun MerchantDetailContent(
    merchant: MerchantDetail,
    onImageClick: (Int) -> Unit,
    onLoginClick: () -> Unit,
    onUpgradeVipClick: () -> Unit,
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
            modifier = Modifier.fillMaxWidth(),
        )

        MerchantContactCard(
            contactAccess = merchant.contactAccess,
            onLoginClick = onLoginClick,
            onUpgradeVipClick = onUpgradeVipClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 展示商家名称、所在省份和详情正文的卡片
 *
 * @param merchant 商家详情领域模型
 * @param modifier 应用于卡片根容器的 [Modifier]
 */
@Composable
private fun MerchantBasicInfoCard(
    merchant: MerchantDetail,
    modifier: Modifier = Modifier,
) {
    // 地区图标跟随字体缩放
    val iconSize = with(LocalDensity.current) { 16.sp.toDp() }

    AppCard(modifier = modifier) {
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

/**
 * 根据 [MerchantContactAccess] 展示联系方式区域，具体样式由各分支自行决定。
 *
 * 四个分支进行显式判断，不使用 else 兜底，新增状态时编译器会提示遗漏；
 * [MerchantContactAccess.Unavailable] 不展示联系方式区域。
 *
 * @param contactAccess 当前用户查看联系方式的权限状态
 * @param onLoginClick 点击登录引导按钮时执行的操作
 * @param onUpgradeVipClick 点击升级 VIP 引导按钮时执行的操作
 * @param modifier 应用于卡片根容器的 [Modifier]
 */
@Composable
private fun MerchantContactCard(
    contactAccess: MerchantContactAccess,
    onLoginClick: () -> Unit,
    onUpgradeVipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (contactAccess) {
        is MerchantContactAccess.Available ->
            AppCard(modifier = modifier) {
                Text(
                    text = contactAccess.contact,
                    style = VeylineTextStyles.Body,
                    color = VeylineTheme.colors.textContent,
                    modifier = Modifier.fillMaxWidth()
                )
            }

        MerchantContactAccess.LoginRequired ->
            ContactLoginRequiredCard(
                onLoginClick = onLoginClick,
                modifier = modifier,
            )

        MerchantContactAccess.VipRequired ->
            ContactVipRequiredCard(
                onUpgradeVipClick = onUpgradeVipClick,
                modifier = modifier,
            )

        MerchantContactAccess.Unavailable -> Unit
    }
}

/**
 * 未登录时的联系方式提示卡片，引导用户登录
 *
 * @param onLoginClick 点击登录按钮时执行的操作
 * @param modifier 应用于卡片根容器的 [Modifier]
 */
@Composable
private fun ContactLoginRequiredCard(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpacingLarge),
        ) {
            Text(
                text = stringResource(R.string.merchant_contact_login_required),
                fontWeight = FontWeight.Bold,
                style = VeylineTextStyles.Body,
                color = VeylineTheme.colors.textContent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            AppButton(
                text = stringResource(R.string.detail_action_login_now),
                onClick = onLoginClick,
                modifier = Modifier
                    .widthIn(min = 140.dp),
            )
        }
    }
}

/**
 * 已登录但非 VIP 时的联系方式提示卡片，引导用户升级 VIP
 *
 * @param onUpgradeVipClick 点击升级 VIP 按钮时执行的操作
 * @param modifier 应用于卡片根容器的 [Modifier]
 */
@Composable
private fun ContactVipRequiredCard(
    onUpgradeVipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpacingLarge),
        ) {
            Text(
                text = stringResource(R.string.merchant_contact_vip_required),
                fontWeight = FontWeight.Bold,
                style = VeylineTextStyles.Body,
                color = VeylineTheme.colors.textContent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            AppButton(
                text = stringResource(R.string.detail_action_upgrade_vip_now),
                onClick = onUpgradeVipClick,
                modifier = Modifier
                    .widthIn(min = 140.dp),
            )
        }
    }
}

// ===== Preview 组件 =====

/** 构造 Preview 用的商家详情数据，联系方式状态由调用方指定 */
private fun previewMerchantDetail(
    contactAccess: MerchantContactAccess,
): MerchantDetail =
    MerchantDetail(
        id = "merchant-a",
        name = "示例商家",
        provinceCode = "110000",
        imageUrls = listOf(
            "https://example.com/image-1.jpg",
            "https://example.com/image-2.jpg",
        ),
        description = "这里是商家详情正文，用于检查较长文字在卡片中的换行、间距和整体展示效果。",
        contactAccess = contactAccess,
    )

@Preview(name = "商家详情 - 可查看联系方式", showSystemUi = true)
@Preview(name = "商家详情 - 可查看联系方式 - 暗色", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantDetailContactAvailablePreview() {
    val merchant = previewMerchantDetail(
        contactAccess = MerchantContactAccess.Available(
            contact = "电话：13800000000\n微信：veyline_example",
        )
    )

    VeylineTheme {
        MerchantDetailScreen(
            uiState = MerchantDetailUiState(
                merchant = merchant,
            ),
            onBackClick = {},
            onRetryClick = {},
            onRefresh = {},
            onImageClick = { _, _ -> },
            onLoginClick = {},
            onUpgradeVipClick = {},
        )
    }
}

@Preview(name = "商家详情 - 需要登录", showSystemUi = true)
@Preview(name = "商家详情 - 需要登录 - 暗色", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantDetailLoginRequiredPreview() {
    VeylineTheme {
        MerchantDetailScreen(
            uiState = MerchantDetailUiState(
                merchant = previewMerchantDetail(
                    contactAccess = MerchantContactAccess.LoginRequired,
                ),
            ),
            onBackClick = {},
            onRetryClick = {},
            onRefresh = {},
            onImageClick = { _, _ -> },
            onLoginClick = {},
            onUpgradeVipClick = {},
        )
    }
}

@Preview(name = "商家详情 - 需要升级 VIP", showSystemUi = true)
@Preview(name = "商家详情 - 需要升级 VIP - 暗色", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantDetailVipRequiredPreview() {
    VeylineTheme {
        MerchantDetailScreen(
            uiState = MerchantDetailUiState(
                merchant = previewMerchantDetail(
                    contactAccess = MerchantContactAccess.VipRequired,
                ),
            ),
            onBackClick = {},
            onRetryClick = {},
            onRefresh = {},
            onImageClick = { _, _ -> },
            onLoginClick = {},
            onUpgradeVipClick = {},
        )
    }
}

@Preview(name = "商家详情 - 大字体", showSystemUi = true, fontScale = 2f)
@Composable
private fun MerchantDetailLargeFontPreview() {
    // 使用 VipRequired 因为升级提示和按钮文案更长，更容易暴露布局问题
    // 去掉图片，确保联系方式区域在首屏内可见
    val merchant = previewMerchantDetail(
        contactAccess = MerchantContactAccess.VipRequired,
    ).copy(
        imageUrls = emptyList(),
    )

    VeylineTheme {
        MerchantDetailScreen(
            uiState = MerchantDetailUiState(
                merchant = merchant,
            ),
            onBackClick = {},
            onRetryClick = {},
            onRefresh = {},
            onImageClick = { _, _ -> },
            onLoginClick = {},
            onUpgradeVipClick = {},
        )
    }
}
