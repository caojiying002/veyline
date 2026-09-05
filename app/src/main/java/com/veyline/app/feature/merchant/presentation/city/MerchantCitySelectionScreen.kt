package com.veyline.app.feature.merchant.presentation.city

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
import com.veyline.app.feature.merchant.domain.model.MerchantCity
import com.veyline.app.ui.components.AppBackTopBar
import com.veyline.app.ui.components.AppEmptyContent
import com.veyline.app.ui.components.AppErrorContent
import com.veyline.app.ui.components.AppLoadingContent
import com.veyline.app.ui.components.CitySelectionListDivider
import com.veyline.app.ui.components.CitySelectionListItem
import com.veyline.app.ui.error.UiError
import com.veyline.app.ui.theme.VeylineTheme

private const val VIEW_MODEL_KEY = "merchant:city_selection"

/**
 * 商家城市选择页面的有状态入口。
 *
 * 负责获取 Hilt 管理的 ViewModel、以生命周期感知的方式收集页面状态，并将首次加载和
 * 重试操作转交给 ViewModel。页面使用固定的专属 key，避免同一个 ViewModelStoreOwner
 * 下的其他页面意外复用该 ViewModel 实例。
 *
 * @param onNavigateBack 请求返回上一页时执行的导航操作。
 * @param modifier 传递给无状态页面根容器的 [Modifier]。
 * @param viewModel 商家城市选择页面的 ViewModel，默认由 Hilt 提供。
 */
@Composable
fun MerchantCitySelectionRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MerchantCitySelectionViewModel = hiltViewModel(
        key = VIEW_MODEL_KEY,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.onAction(MerchantCitySelectionAction.InitialLoad)
    }

    MerchantCitySelectionScreen(
        uiState = uiState,
        onBackClick = onNavigateBack,
        onRetryClick = {
            viewModel.onAction(MerchantCitySelectionAction.Retry)
        },
        modifier = modifier,
    )
}

/**
 * 商家城市选择页面的无状态 UI。
 *
 * 页面根据 [uiState] 展示全屏加载、全屏错误、空数据或城市列表。已有城市内容时始终保留
 * 列表，不使用全屏状态覆盖，便于后续扩展刷新和分页状态。
 *
 * 标题栏负责处理顶部状态栏 Insets；页面主体分别为不可滚动状态内容和 [LazyColumn]
 * 处理底部安全区域，确保三键导航和手势导航下的内容均不被遮挡。
 *
 * @param uiState 当前页面状态。
 * @param onBackClick 点击标题栏返回区域时执行的操作。
 * @param onRetryClick 在全屏错误状态下点击重试按钮时执行的操作。
 * @param modifier 应用于页面根容器的 [Modifier]。
 */
@Composable
fun MerchantCitySelectionScreen(
    uiState: MerchantCitySelectionUiState,
    onBackClick: () -> Unit,
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
            title = stringResource(R.string.merchant_city_selection_title),
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

                // 已排除加载和错误，剩余的无内容情况即为空状态
                !uiState.hasContent -> {
                    AppEmptyContent(
                        message = stringResource(R.string.merchant_city_empty),
                        modifier = Modifier.windowInsetsPadding(bottomInsets),
                    )
                }

                // 其余情况均已有内容，保留并展示城市列表
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = bottomInsets.asPaddingValues(),
                    ) {
                        itemsIndexed(
                            items = uiState.cities,
                            key = { _, city -> city.code },
                        ) { index, city ->
                            CitySelectionListItem(
                                cityName = city.name,
                            )

                            if (index < uiState.cities.lastIndex) {
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
@Preview(name = "城市列表", showSystemUi = true)
@Preview(name = "城市列表 - 暗色", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantCitySelectionContentPreview() {
    val cities = listOf(
        MerchantCity(
            code = "110000",
            name = "北京市",
        ),
        MerchantCity(
            code = "310000",
            name = "上海市",
        ),
        MerchantCity(
            code = "650000",
            name = "新疆维吾尔自治区",
        ),
    )

    VeylineTheme {
        MerchantCitySelectionScreen(
            uiState = MerchantCitySelectionUiState(
                cities = cities,
                isLoading = false,
                error = null,
            ),
            onBackClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "加载状态", showSystemUi = true)
@Preview(name = "加载状态 - 暗色", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantCitySelectionLoadingPreview() {
    VeylineTheme {
        MerchantCitySelectionScreen(
            uiState = MerchantCitySelectionUiState(
                cities = emptyList(),
                isLoading = true,
                error = null,
            ),
            onBackClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "错误状态", showSystemUi = true)
@Preview(name = "错误状态 - 暗色", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantCitySelectionErrorPreview() {
    VeylineTheme {
        MerchantCitySelectionScreen(
            uiState = MerchantCitySelectionUiState(
                cities = emptyList(),
                isLoading = false,
                error = UiError.Connection,
            ),
            onBackClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "空状态", showSystemUi = true)
@Preview(name = "空状态 - 暗色", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantCitySelectionEmptyPreview() {
    VeylineTheme {
        MerchantCitySelectionScreen(
            uiState = MerchantCitySelectionUiState(
                cities = emptyList(),
                isLoading = false,
                error = null,
            ),
            onBackClick = {},
            onRetryClick = {},
        )
    }
}
