package com.veyline.app.feature.merchant.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.veyline.app.ui.components.CitySelectionTopBar
import com.veyline.app.ui.theme.VeylineTheme



@Composable
fun MerchantListScreen(
    uiState: MerchantListUiState,
    onCitySelectionClick: () -> Unit,
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

        }
    }
}
