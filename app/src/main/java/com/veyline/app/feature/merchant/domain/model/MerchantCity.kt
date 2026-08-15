package com.veyline.app.feature.merchant.domain.model

/**
 * 可供商家业务与 UI 直接使用的城市信息。
 *
 * 本模型只包含经过 data 层校验的稳定字段，不直接承载服务端响应中的可空数据。
 *
 * @property code 城市的业务标识。
 * @property name 向用户展示的城市名称。
 */
data class MerchantCity(
    val code: String,
    val name: String,
)
