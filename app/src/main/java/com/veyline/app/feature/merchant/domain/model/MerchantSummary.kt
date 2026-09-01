package com.veyline.app.feature.merchant.domain.model

/**
 * 商家列表使用的商家摘要领域模型。
 *
 * 该模型只包含列表展示和后续导航所需的数据。属性均经过数据层校验和清洗：必要字段
 * 保持有效的非空内容，可选字段转换为明确的空值语义。模型不包含商家详情页专用信息，
 * ViewModel 和 UI 应使用本类型，而不是直接依赖网络 DTO。
 *
 * @property id 商家唯一标识，用于列表项去重和后续导航。
 * @property name 经过清理的商家名称。
 * @property cityCode 经过校验的地区代码，用于转换列表展示的地区名称。
 * @property intro 经过清理的商家简介；服务端未提供有效内容时为空字符串。
 * @property coverImageUrl 可直接请求的封面图片完整地址；没有有效图片路径时为 `null`。
 */
data class MerchantSummary(
    val id: String,
    val name: String,
    val cityCode: String,
    val intro: String,
    val coverImageUrl: String?,
)
