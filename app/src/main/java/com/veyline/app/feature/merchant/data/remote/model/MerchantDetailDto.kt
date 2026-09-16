package com.veyline.app.feature.merchant.data.remote.model

import com.squareup.moshi.JsonClass

/**
 * 商家详情接口返回的网络模型。
 *
 * 本类型只声明详情页展示所需的字段，不包含商家列表使用的摘要信息。详情与列表使用独立
 * DTO，避免通过大量可空字段判断同一个对象来自哪个接口。
 *
 * 所有字段保持可空，使服务端漏传字段或返回 JSON `null` 时仍能完成解析。后续 Mapper
 * 负责校验必要字段、清理可选内容并转换为具有非空约束的领域模型；DTO 不应直接进入
 * ViewModel 或 UI。
 *
 * @property id 服务端返回的商家唯一标识
 * @property name 服务端返回的商家名称
 * @property cityCode 服务端协议统一使用的地区筛选代码；商家详情中实际表示省级行政区
 * @property picture 服务端返回的图片相对路径，多张图片以英文逗号分隔；允许缺失或为空
 * @property desc 服务端返回的商家详情正文；允许缺失或为空
 * @property contact 服务端返回的联系方式；仅 VIP 或已用积分购买的用户可见，其他情况下缺失或为空
 */
@JsonClass(generateAdapter = true)
data class MerchantDetailDto(
    val id: String?,
    val name: String?,
    val cityCode: String?,
    val picture: String?,
    val desc: String?,
    val contact: String?,
)
