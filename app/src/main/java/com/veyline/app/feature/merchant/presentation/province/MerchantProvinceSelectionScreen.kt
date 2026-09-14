package com.veyline.app.feature.merchant.presentation.province

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veyline.app.R
import com.veyline.app.feature.merchant.domain.model.MerchantProvince
import com.veyline.app.ui.components.AppBackTopBar
import com.veyline.app.ui.components.AppErrorContent
import com.veyline.app.ui.components.AppLoadingContent
import com.veyline.app.ui.components.CitySelectionListDivider
import com.veyline.app.ui.components.CitySelectionListItem
import com.veyline.app.ui.error.UiError
import com.veyline.app.ui.theme.VeylineTheme

private const val VIEW_MODEL_KEY = "merchant:province_selection"

/**
 * 商家省份选择页面的有状态入口。
 *
 * 负责获取 Hilt 管理的 ViewModel、以生命周期感知的方式收集页面状态，并将首次加载和
 * 重试操作转交给 ViewModel。用户点击省份后，选择结果继续向上传给导航层，
 * Route 本身不保存筛选状态，也不直接操作导航栈。
 *
 * 页面使用固定的专属 key，避免同一个 ViewModelStoreOwner 下的其他页面意外复用
 * 该 ViewModel 实例。
 *
 * @param onNavigateBack 请求返回上一页时执行的导航操作。
 * @param onProvinceSelected 用户选中省份时执行的回调，参数为当前选中的省份，null 表示选择“全国”。
 * @param modifier 传递给无状态页面根容器的 [Modifier]。
 * @param viewModel 商家省份选择页面的 ViewModel，默认由 Hilt 提供。
 */
@Composable
fun MerchantProvinceSelectionRoute(
    onNavigateBack: () -> Unit,
    onProvinceSelected: (MerchantProvince?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MerchantProvinceSelectionViewModel = hiltViewModel(
        key = VIEW_MODEL_KEY,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.onAction(MerchantProvinceSelectionAction.InitialLoad)
    }

    MerchantProvinceSelectionScreen(
        uiState = uiState,
        onBackClick = onNavigateBack,
        onProvinceClick = onProvinceSelected,
        onRetryClick = {
            viewModel.onAction(MerchantProvinceSelectionAction.Retry)
        },
        modifier = modifier,
    )
}

/**
 * 商家省份选择页面的无状态 UI。
 *
 * 页面根据 [uiState] 展示全屏加载、全屏错误或省份列表。列表顶部固定展示“全国”选项，
 * 不依赖省份数据即可选择；服务端返回的省份列表为空时，列表仅包含“全国”一项，
 * 不使用独立的全屏空状态覆盖整页。
 *
 * 标题栏负责处理顶部状态栏 Insets；页面主体分别为不可滚动状态内容和 [LazyColumn]
 * 处理底部安全区域，确保三键导航和手势导航下的内容均不被遮挡。
 *
 * @param uiState 当前页面状态。
 * @param onBackClick 点击标题栏返回区域时执行的操作。
 * @param onProvinceClick 点击列表项时执行的操作，参数为选中的省份，null 表示选择“全国”。
 * @param onRetryClick 在全屏错误状态下点击重试按钮时执行的操作。
 * @param modifier 应用于页面根容器的 [Modifier]。
 */
@Composable
fun MerchantProvinceSelectionScreen(
    uiState: MerchantProvinceSelectionUiState,
    onBackClick: () -> Unit,
    onProvinceClick: (MerchantProvince?) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomInsets = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Bottom)
    val uiError: UiError? = uiState.error

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VeylineTheme.colors.background),
    ) {
        AppBackTopBar(
            title = stringResource(R.string.city_selection_title),
            onBackClick = onBackClick,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when {
                // 已包含“正在加载且无内容”的判断，优先显示全屏加载状态
                uiState.isInitialLoading -> {
                    AppLoadingContent(
                        modifier = Modifier.windowInsetsPadding(bottomInsets),
                    )
                }

                // 已排除无内容加载；无内容且存在错误时显示全屏错误状态
                uiError != null && !uiState.hasContent -> {
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

                // 已排除加载和错误，剩余情况本应是独立空状态；本项目用“全国”兜底空列表
                // （见下方 else 分支），这段分支目前不会被执行到。保留完整的四态判断
                // （加载/错误/空/内容）作为后续页面的参考实现——当某个新页面确实需要
                // 独立空状态时可以直接参考这里。项目开发过几个正式页面、这段参考价值
                // 消退后，可以回来删除。
                /*
                !uiState.hasContent -> {
                    AppEmptyContent(
                        message = stringResource(R.string.merchant_province_empty),
                        modifier = Modifier.windowInsetsPadding(bottomInsets),
                    )
                }
                */

                // 已排除加载和错误：展示列表。“全国”固定为第一项，是纯 UI 层概念，
                // 只在这里拼接展示，不进入 uiState.provinces 或领域模型；
                // provinces 为空时列表也仅含“全国”一项，不会显示为空白
                else -> {
                    val provinceOptions: List<MerchantProvince?> = listOf(null) + uiState.provinces

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = bottomInsets.asPaddingValues(),
                    ) {
                        itemsIndexed(
                            items = provinceOptions,
                            key = { _, province -> province?.code ?: "all" },
                        ) { index, province ->
                            CitySelectionListItem(
                                cityName = province?.name ?: stringResource(R.string.merchant_province_all),
                                onClick = { onProvinceClick(province) },
                            )

                            if (index < provinceOptions.lastIndex) {
                                CitySelectionListDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===== Preview 组件 =====
// Screen 内部会自动在列表顶部拼接“全国”，预览数据不需要另外包含这一项
@Preview(name = "商家省份列表", showSystemUi = true)
@Preview(name = "商家省份列表 - 暗色", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantProvinceSelectionContentPreview() {
    val cities = listOf(
        MerchantProvince(
            code = "110000",
            name = "北京市",
        ),
        MerchantProvince(
            code = "310000",
            name = "上海市",
        ),
        MerchantProvince(
            code = "650000",
            name = "新疆维吾尔自治区",
        ),
    )

    VeylineTheme {
        MerchantProvinceSelectionScreen(
            uiState = MerchantProvinceSelectionUiState(
                provinces = cities,
                isLoading = false,
                error = null,
            ),
            onBackClick = {},
            onProvinceClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "加载状态", showSystemUi = true)
@Preview(name = "加载状态 - 暗色", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantProvinceSelectionLoadingPreview() {
    VeylineTheme {
        MerchantProvinceSelectionScreen(
            uiState = MerchantProvinceSelectionUiState(
                provinces = emptyList(),
                isLoading = true,
                error = null,
            ),
            onBackClick = {},
            onProvinceClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "错误状态", showSystemUi = true)
@Preview(name = "错误状态 - 暗色", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantProvinceSelectionErrorPreview() {
    VeylineTheme {
        MerchantProvinceSelectionScreen(
            uiState = MerchantProvinceSelectionUiState(
                provinces = emptyList(),
                isLoading = false,
                error = UiError.Connection,
            ),
            onBackClick = {},
            onProvinceClick = {},
            onRetryClick = {},
        )
    }
}

// 服务端返回空列表的场景：不使用独立空状态，显示只含“全国”一项的列表
@Preview(name = "无省份数据", showSystemUi = true)
@Preview(name = "无省份数据 - 暗色", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantProvinceSelectionNoDataPreview() {
    VeylineTheme {
        MerchantProvinceSelectionScreen(
            uiState = MerchantProvinceSelectionUiState(
                provinces = emptyList(),
                isLoading = false,
                error = null,
            ),
            onBackClick = {},
            onProvinceClick = {},
            onRetryClick = {},
        )
    }
}
