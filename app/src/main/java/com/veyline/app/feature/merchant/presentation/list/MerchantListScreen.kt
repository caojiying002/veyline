package com.veyline.app.feature.merchant.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.veyline.app.R
import com.veyline.app.data.paging.PagingFailureException
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import com.veyline.app.ui.components.AppEmptyContent
import com.veyline.app.ui.components.AppErrorContent
import com.veyline.app.ui.components.AppLoadingContent
import com.veyline.app.ui.components.AppPagingErrorFooter
import com.veyline.app.ui.components.AppPagingLoadingFooter
import com.veyline.app.ui.components.CitySelectionTopBar
import com.veyline.app.ui.error.UiError
import com.veyline.app.ui.error.toUiError
import com.veyline.app.ui.theme.DefaultHorizontalSpace
import com.veyline.app.ui.theme.DividerHeight
import com.veyline.app.ui.theme.VeylineTheme

private const val VIEW_MODEL_KEY = "merchant:list"

/**
 * 商家列表的有状态入口。
 *
 * 负责获取 ViewModel、收集页面状态和分页数据，并连接外部导航回调。
 * 当前进入组合时即视为页面可见，首次加载由 ViewModel 保证幂等。
 *
 * @param onNavigateToCitySelection 打开城市选择页面
 * @param onNavigateToMerchantDetail 根据商家 ID 打开详情页面
 * @param modifier 传递给页面根布局的 Modifier
 * @param viewModel 商家列表 ViewModel，默认由 Hilt 提供
 */
@Composable
fun MerchantListRoute(
    onNavigateToCitySelection: () -> Unit,
    onNavigateToMerchantDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MerchantListViewModel = hiltViewModel(
        key = VIEW_MODEL_KEY,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val merchants = viewModel.merchants.collectAsLazyPagingItems()

    LaunchedEffect(viewModel) {
        viewModel.onAction(MerchantListAction.InitialLoad)
    }

    MerchantListScreen(
        uiState = uiState,
        merchants = merchants,
        onCitySelectionClick = onNavigateToCitySelection,
        onMerchantClick = { merchant ->
            onNavigateToMerchantDetail(merchant.id)
        },
        modifier = modifier,
    )
}

/**
 * 商家列表页面，展示城市筛选入口、分页列表及加载状态。
 *
 * [uiState] 提供当前城市等非分页状态，列表数据和加载状态由 [merchants] 提供，
 * 不在页面内另行维护。组件不直接获取 ViewModel，也不持有导航控制器。
 *
 * 没有列表内容时，根据首次加载结果显示加载、错误重试或空态；已有内容时保留列表，
 * 分页加载和失败重试只在列表底部展示。全屏错误显示转换后的错误文案，分页失败使用
 * 固定的简短提示，两处重试均交给 Paging 处理。
 *
 * 标题栏负责顶部状态栏避让，底部和横向系统安全区域由宿主处理。
 * 当前尚未接入下拉刷新，已有内容时的刷新反馈留待后续补充。
 *
 * @param uiState 页面非分页状态，用于展示当前选中的城市
 * @param merchants 分页列表数据及加载状态，页面通过索引访问条目以触发分页预取
 * @param onCitySelectionClick 点击城市选择区域时的回调
 * @param onMerchantClick 点击商家列表项时的回调，参数为当前商家
 * @param modifier 应用于页面根布局的 Modifier
 */
@Composable
fun MerchantListScreen(
    uiState: MerchantListUiState,
    merchants: LazyPagingItems<MerchantSummary>,
    onCitySelectionClick: () -> Unit,
    onMerchantClick: (MerchantSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VeylineTheme.colors.background),
    ) {
        // 标题栏负责顶部状态栏避让
        CitySelectionTopBar(
            cityName = uiState.selectedCity?.name,
            onCitySelectionClick = onCitySelectionClick,
        )

        // 后续在这里接入列表及加载、错误、空状态
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val refreshState = merchants.loadState.refresh
            val appendState = merchants.loadState.append

            when {
                // 已有内容时保留列表，刷新或分页失败不替换整页
                merchants.itemCount > 0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = DefaultHorizontalSpace,
                            vertical = DividerHeight,
                        ),
                        verticalArrangement = Arrangement.spacedBy(DividerHeight)
                    ) {
                        items(
                            count = merchants.itemCount,
                            key = merchants.itemKey { it.id },
                        ) { index ->
                            val merchant = merchants[index]

                            if (merchant != null) {
                                MerchantListItem(
                                    merchant = merchant,
                                    onClick = { onMerchantClick(merchant) },
                                )
                            }
                        }

                        // 分页状态只在列表底部展示，不影响已有内容
                        when (appendState) {
                            LoadState.Loading -> {
                                item { AppPagingLoadingFooter() }
                            }
                            is LoadState.Error -> {
                                item { AppPagingErrorFooter({ merchants.retry() }) }
                            }
                            is LoadState.NotLoading -> Unit
                        }
                    }
                }

                // 当前没有列表内容，首次加载时显示整页加载提示
                refreshState is LoadState.Loading -> {
                    AppLoadingContent()
                }

                // 当前没有列表内容，加载失败时显示错误原因并提供重试入口
                refreshState is LoadState.Error -> {
                    val errorMessage = pagingErrorMessage(refreshState.error)

                    AppErrorContent(
                        message = errorMessage,
                        onRetryClick = { merchants.retry() },
                    )
                }

                // 当前没有列表内容，且加载已结束、没有后续数据时才显示空态
                (refreshState is LoadState.NotLoading &&
                        appendState.endOfPaginationReached) -> {
                    AppEmptyContent(
                        message = stringResource(R.string.merchant_list_empty),
                    )
                }
            }
        }
    }
}

/**
 * 将分页加载异常转换为全屏错误提示文案。
 */
@Composable
private fun pagingErrorMessage(error: Throwable): String {
    val uiError = (error as? PagingFailureException)?.toUiError()
        ?: UiError.Technical

    return when (uiError) {
        UiError.Connection -> stringResource(R.string.error_connection)
        UiError.Technical -> stringResource(R.string.error_technical)
        is UiError.DisplayReady -> uiError.message
    }
}
