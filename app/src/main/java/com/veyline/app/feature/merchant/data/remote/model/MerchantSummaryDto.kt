package com.veyline.app.feature.merchant.data.remote.model

import com.squareup.moshi.JsonClass

/**
 * 商家列表接口返回的商家摘要网络模型。
 *
 * 本类型只声明列表展示所需的字段，不包含商家详情页使用的联系方式、详情正文或其他状态
 * 信息。列表与详情使用独立 DTO，避免通过大量可空字段判断同一个对象来自哪个接口。
 *
 * 所有字段保持可空，使服务端漏传字段或返回 JSON `null` 时仍能完成单条数据的解析。后续
 * Mapper 负责校验必要字段、清理可选内容并转换为具有非空约束的领域模型；DTO 不应直接
 * 进入 ViewModel 或 UI。
 *
 * @property id 服务端返回的商家唯一标识；列表项去重和后续导航需要该字段。
 * @property name 服务端返回的商家名称。
 * @property cityCode 服务端返回的地区代码，用于转换列表展示的地区名称。
 * @property intro 服务端返回的商家简介；允许缺失或为空。
 * @property coverPicture 服务端返回的封面图片相对路径；允许缺失或为空，不包含图片域名。
 */
@JsonClass(generateAdapter = true)
data class MerchantSummaryDto(
    val id: String?,
    val name: String?,
    val cityCode: String?,
    val intro: String?,
    val coverPicture: String?,
)
