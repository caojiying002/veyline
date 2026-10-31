package com.veyline.app.feature.profile.data.remote.model

import com.squareup.moshi.JsonClass

/**
 * 当前用户资料接口返回的网络模型。
 *
 * 只声明个人中心页展示所需的字段，不是完整的接口响应；服务端返回的其他字段会被忽略。
 * 所有字段保持可空，必要字段的校验、展示字段的降级及会员状态转换由 Mapper 负责；
 * 本类型不应直接进入 ViewModel 或 UI。
 *
 * @property id 用户唯一标识，使用字符串承载
 * @property name 用户名
 * @property score 用户积分
 * @property publishedInfoCount 已发布的信息数量
 * @property isVip 是否 VIP 用户
 * @property expiredAt VIP 到期毫秒时间戳；永久会员使用约定的特殊值表示
 */
@JsonClass(generateAdapter = true)
data class UserProfileDto(
    val id: String?,
    val name: String?,
    val score: Int?,
    val publishedInfoCount: Int?,
    val isVip: Boolean?,
    val expiredAt: Long?,
)
