package com.veyline.app.feature.merchant.data.remote.model

import com.squareup.moshi.JsonClass

/**
 * 商家城市接口返回的网络模型。
 *
 * 字段保持可空，使服务端漏传字段或返回 `null` 时仍能完成 JSON 解析，并由 Repository
 * 结合业务语义统一校验和转换，避免不稳定的网络数据直接进入 UI。
 *
 * @property code 服务端返回的城市标识。
 * @property name 服务端返回的城市名称。
 */
@JsonClass(generateAdapter = true)
data class MerchantCityDto(
    val code: String?,
    val name: String?,
)
