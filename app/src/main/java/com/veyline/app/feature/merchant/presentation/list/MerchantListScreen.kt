package com.veyline.app.feature.merchant.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.veyline.app.R
import com.veyline.app.data.paging.PagingFailureException
import com.veyline.app.feature.merchant.domain.model.MerchantProvince
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import com.veyline.app.navigation.MERCHANT_PROVINCE_SELECTION_RESULT_KEY
import com.veyline.app.navigation.MerchantProvinceSelectionResult
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
 * [MerchantListViewModel.merchants] 一旦在这里被订阅就会立即按当前筛选条件发起请求。
 *
 * @param onNavigateToProvinceSelection 打开地区选择页面
 * @param onNavigateToMerchantDetail 根据商家 ID 打开详情页面
 * @param modifier 传递给页面根布局的 Modifier
 * @param viewModel 商家列表 ViewModel，默认由 Hilt 提供
 */
@Composable
fun MerchantListRoute(
    savedStateHandle: SavedStateHandle,
    onNavigateToProvinceSelection: () -> Unit,
    onNavigateToMerchantDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MerchantListViewModel = hiltViewModel(
        key = VIEW_MODEL_KEY,
    ),
) {
    val provinceSelectionResult by savedStateHandle
        .getStateFlow<MerchantProvinceSelectionResult?>(MERCHANT_PROVINCE_SELECTION_RESULT_KEY, null)
        .collectAsStateWithLifecycle()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(provinceSelectionResult, viewModel) {
        val result = provinceSelectionResult ?: return@LaunchedEffect

        viewModel.onAction(
            if (result.code != null && result.name != null)
                MerchantListAction.SelectProvince(MerchantProvince(result.code, result.name))
            else
                MerchantListAction.SelectAllProvinces
        )

        // 【重要】消费后把值置空
        savedStateHandle[MERCHANT_PROVINCE_SELECTION_RESULT_KEY] = null
    }

    /*LaunchedEffect(viewModel) {
        viewModel.onAction(MerchantListAction.InitialLoad)
    }*/

    // 按 selectedProvince 重建这个子树：不这样做的话，切换地区后 Paging 只是在旧的
    // LazyPagingItems 内部悄悄换源，标题栏会立刻显示新地区，但列表在加载/报错期间
    // 仍展示旧地区已缓存的商家数据，出现"标题与内容不一致"的中间态。用 key() 强制
    // 连同 collectAsLazyPagingItems() 一起丢弃重建，让标题栏和列表内容随同一次地区
    // 切换一起更新
    key(uiState.selectedProvince, viewModel) {
        val merchants = viewModel.merchants.collectAsLazyPagingItems()

        MerchantListScreen(
            uiState = uiState,
            merchants = merchants,
            onProvinceSelectionClick = onNavigateToProvinceSelection,
            onMerchantClick = { merchant ->
                onNavigateToMerchantDetail(merchant.id)
            },
            modifier = modifier,
        )
    }
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
 * 仅在已有列表内容时提供下拉刷新；首次加载、全屏错误和空态不响应下拉操作。
 * 刷新状态直接由 Paging 驱动，刷新失败时保留已有列表。
 *
 * @param uiState 页面非分页状态，用于展示当前选中的城市
 * @param merchants 分页列表数据及加载状态，页面通过索引访问条目以触发分页预取
 * @param onProvinceSelectionClick 点击地区选择区域时的回调
 * @param onMerchantClick 点击商家列表项时的回调，参数为当前商家
 * @param modifier 应用于页面根布局的 Modifier
 */
@Composable
fun MerchantListScreen(
    uiState: MerchantListUiState,
    merchants: LazyPagingItems<MerchantSummary>,
    onProvinceSelectionClick: () -> Unit,
    onMerchantClick: (MerchantSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VeylineTheme.colors.background),
    ) {
        // 标题栏负责顶部状态栏 Insets
        CitySelectionTopBar(
            cityName = uiState.selectedProvince?.name,
            onCitySelectionClick = onProvinceSelectionClick,
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
                // 尚未触发首次加载，保持内容区域空白
                !uiState.hasTriggeredInitialLoad -> Unit

                // 已有内容时保留列表，刷新或分页失败不替换整页
                merchants.itemCount > 0 -> {
                    PullToRefreshBox(
                        isRefreshing = refreshState is LoadState.Loading,
                        onRefresh = { merchants.refresh() },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
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
