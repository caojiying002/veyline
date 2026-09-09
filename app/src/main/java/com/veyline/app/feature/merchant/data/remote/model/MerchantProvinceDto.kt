package com.veyline.app.feature.merchant.data.remote.model

import com.squareup.moshi.JsonClass

/**
 * 商家省份接口返回的网络模型。
 *
 * 接口沿用服务端与其他业务共用的 `city` 命名，实际返回的是省级地区数据。
 *
 * 字段全部可空：服务端漏传或返回 `null` 时仍能完成 JSON 解析，具体校验和转换交给
 * Repository 按业务语义统一处理，不让不稳定的网络数据直接进入 UI。
 *
 * @property code 服务端返回的省份标识。
 * @property name 服务端返回的省份名称。
 */
@JsonClass(generateAdapter = true)
data class MerchantProvinceDto(
    val code: String?,
    val name: String?,
)
