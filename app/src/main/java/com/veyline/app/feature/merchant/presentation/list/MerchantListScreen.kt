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
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import com.veyline.app.ui.components.CitySelectionTopBar
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

            LazyColumn(
                modifier = Modifier,
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
    }
}
