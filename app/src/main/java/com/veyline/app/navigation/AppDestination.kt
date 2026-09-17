package com.veyline.app.navigation

import kotlinx.serialization.Serializable

/**
 * 应用内所有导航目的地的类型安全路由定义。
 *
 * 统一使用 "Destination" 后缀命名，跟"Route"专指有状态 Composable 入口（如
 * [com.veyline.app.feature.merchant.presentation.list.MerchantListRoute]）的用法
 * 区分开，避免两个概念混淆。
 */

/** 商家列表页 */
@Serializable
data object MerchantListDestination

/**
 * 商家详情页
 *
 * @property merchantId 要展示的商家 ID
 */
@Serializable
data class MerchantDetailDestination(
    val merchantId: String,
)

/** 商家省份选择页 */
@Serializable
data object MerchantProvinceSelectionDestination
