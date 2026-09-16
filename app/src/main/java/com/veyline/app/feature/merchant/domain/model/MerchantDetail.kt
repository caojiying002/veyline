package com.veyline.app.feature.merchant.domain.model

/**
 * 商家详情页使用的商家详情领域模型。
 *
 * 该模型只包含详情页展示所需的数据，属性均经过数据层校验和清洗：必要字段保持有效的
 * 非空内容，可选字段转换为明确的空值语义。模型不包含商家列表使用的摘要信息，
 * ViewModel 和 UI 应使用本类型，而不是直接依赖网络 DTO。
 *
 * @property id 商家唯一标识
 * @property name 经过清理的商家名称
 * @property provinceCode 由网络模型的 `cityCode` 转换而来的省级行政区代码
 * @property imageUrls 可直接请求的图片完整地址列表；没有有效图片时为空列表
 * @property description 经过清理的商家详情正文；服务端未提供有效内容时为 `null`
 * @property contact 经过清理的联系方式；仅 VIP 或已用积分购买的用户可见，其他情况下为 `null`
 */
data class MerchantDetail(
    val id: String,
    val name: String,
    val provinceCode: String,
    val imageUrls: List<String>,
    val description: String?,
    val contact: String?,
)
