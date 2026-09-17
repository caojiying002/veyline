package com.veyline.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.veyline.app.feature.merchant.presentation.detail.MerchantDetailRoute
import com.veyline.app.feature.merchant.presentation.list.MerchantListRoute
import com.veyline.app.feature.merchant.presentation.province.MerchantProvinceSelectionRoute

/**
 * 应用唯一的导航图，注册所有目的地及其跳转关系。
 *
 * 页面层只通过回调向上抛出用户事件，不直接持有 [navController]；跳转、路由参数解析、
 * 跨页面结果回传等导航相关逻辑集中在这里组装。
 */
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
                // 显式传入这个 entry 的 SavedStateHandle，用于接收省份选择页回传的
                // 结果（写入位置见下方 onProvinceSelected）
                savedStateHandle = backStackEntry.savedStateHandle,
                onNavigateToProvinceSelection = {
                    navController.navigate(MerchantProvinceSelectionDestination)
                },
                onNavigateToMerchantDetail = { merchantId ->
                    navController.navigate(
                        MerchantDetailDestination(merchantId)
                    )
                },
            )
        }

        composable<MerchantDetailDestination> { backStackEntry ->
            // merchantId 作为 AssistedInject 构造参数传给 MerchantDetailViewModel，
            // 这里只负责从路由参数里解析出来，不经过 SavedStateHandle
            val merchantId = backStackEntry
                .toRoute<MerchantDetailDestination>()
                .merchantId

            MerchantDetailRoute(
                merchantId = merchantId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<MerchantProvinceSelectionDestination> {
            MerchantProvinceSelectionRoute(
                onNavigateBack = { navController.popBackStack() },
                onProvinceSelected = { province ->
                    // 写回商家列表页的 SavedStateHandle（即上方传给 MerchantListRoute
                    // 的那个 entry）
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
