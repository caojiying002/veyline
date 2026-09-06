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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.veyline.app.R
import com.veyline.app.data.paging.PagingFailureException
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import com.veyline.app.ui.components.AppEmptyContent
import com.veyline.app.ui.components.AppErrorContent
import com.veyline.app.ui.components.AppLoadingContent
import com.veyline.app.ui.components.CitySelectionTopBar
import com.veyline.app.ui.error.UiError
import com.veyline.app.ui.error.toUiError
import com.veyline.app.ui.theme.DefaultHorizontalSpace
import com.veyline.app.ui.theme.DividerHeight
import com.veyline.app.ui.theme.VeylineTheme



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
