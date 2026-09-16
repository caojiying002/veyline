package com.veyline.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.veyline.app.feature.merchant.presentation.list.MerchantListRoute
import com.veyline.app.feature.merchant.presentation.province.MerchantProvinceSelectionRoute

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MerchantListDestination,
        modifier = modifier,
    ) {
        composable<MerchantListDestination> { backStackEntry ->
            MerchantListRoute(
                savedStateHandle = backStackEntry.savedStateHandle,
                onNavigateToProvinceSelection = {
                    navController.navigate(MerchantProvinceSelectionDestination)
                },
                onNavigateToMerchantDetail = {
                    // TODO 接入商家详情导航
                },
            )
        }

        composable<MerchantProvinceSelectionDestination> {
            MerchantProvinceSelectionRoute(
                onNavigateBack = { navController.popBackStack() },
                onProvinceSelected = { province ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(
                            MERCHANT_PROVINCE_SELECTION_RESULT_KEY,
                            MerchantProvinceSelectionResult(
                                code = province?.code,
                                name = province?.name,
                            ),
                        )

                    navController.popBackStack()
                },
            )
        }
    }
}
