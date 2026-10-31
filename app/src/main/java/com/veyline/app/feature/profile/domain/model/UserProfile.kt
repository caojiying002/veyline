package com.veyline.app.feature.profile.domain.model

/**
 * 当前登录用户的个人资料。
 *
 * 用于个人中心展示，由 Mapper 从网络模型转换而来，所有字段都已经过校验或降级处理，
 * 使用方不需要再判空。
 *
 * 资料中的积分和会员身份仅用于展示，不作为详情访问或购买操作的授权依据，实际权限和交易
 * 仍由服务端判断。
 *
 * @property id 用户唯一标识，已去除首尾空白并保证非空
 * @property name 用户名，已去除首尾空白并保证非空
 * @property score 用户积分，保留服务端数值，包括负数；不作为购买授权依据
 * @property publishedInfoCount 已发布的信息数量，缺失或负数已归零
 * @property membership 用于展示的会员身份及 VIP 有效期；不作为购买授权依据
 */
data class UserProfile(
    val id: String,
    val name: String,
    val score: Int,
    val publishedInfoCount: Int,
    val membership: Membership,
)
