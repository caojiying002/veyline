package com.veyline.app.feature.profile.data.mapper

import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.profile.data.remote.model.UserProfileDto
import com.veyline.app.feature.profile.domain.model.Membership
import com.veyline.app.feature.profile.domain.model.UserProfile
import com.veyline.app.feature.profile.domain.model.VipExpiry

/**
 * 将用户资料网络模型转换为经过校验和清洗的领域模型。
 *
 * 缺少 id、name 或 score 时说明响应本身不完整，无法支持个人中心展示，因此直接抛出
 * [InvalidApiDataException]。积分未知时不能默认成 0，否则会被误当成真实余额；负积分的业务
 * 含义不明，原样保留，不由客户端推断服务端规则。展示负积分不代表允许购买，实际交易由服务端
 * 判断。
 *
 * 发布数量只用于展示，缺失或为负数时按 0 处理，不影响整份资料的显示。
 *
 * 会员身份由 `isVip` 决定，字段缺失时保守地按 [Membership.Regular] 处理，不依据到期时间或
 * 本地时间自行推导。VIP 的到期时间（Unix 毫秒时间戳）转换为 [VipExpiry]：
 * - 缺失或不是正数：[VipExpiry.Unknown]
 * - 等于服务端约定的永久标记时间戳：[VipExpiry.Permanent]
 * - 其余情况：[VipExpiry.Until]
 *
 * [PERMANENT_VIP_EXPIRY_MILLIS] 是服务端使用的特殊时间戳，定义在私有常量中，上层代码不需要认识它。
 */
internal object UserProfileMapper {

    // 服务端约定的永久 VIP 标记，对应北京时间 2038-01-19 00:00:00。
    private const val PERMANENT_VIP_EXPIRY_MILLIS = 2147443200000L

    fun map(profileDto: UserProfileDto): UserProfile {
        val id = profileDto.id?.trim()
        val name = profileDto.name?.trim()
        val score = profileDto.score

        // id、name 和 score 是核心字段，缺失时视为响应不完整；
        // 积分未知不能默认成 0，负积分直接展示但不代表服务端允许积分购买。
        if (id.isNullOrEmpty() || name.isNullOrEmpty() || score == null) {
            throw InvalidApiDataException(
                "User profile response is missing required fields",
            )
        }

        // 发布数量仅用于展示，缺失或负数时按 0 降级，不影响整份用户资料的显示
        val publishedInfoCount = profileDto.publishedInfoCount?.coerceAtLeast(0) ?: 0
        val membership = mapMembership(profileDto.isVip, profileDto.expiredAt)

        return UserProfile(
            id = id,
            name = name,
            score = score,
            publishedInfoCount = publishedInfoCount,
            membership = membership,
        )
    }

    private fun mapMembership(
        isVip: Boolean?,
        expiredAtMillis: Long?,
    ): Membership {
        if (isVip != true) {
            // isVip 字段缺失时保守地按普通用户展示，并忽略到期信息；实际权限仍由服务端判断
            return Membership.Regular
        }

        val vipExpiry: VipExpiry = when {
            (expiredAtMillis == null || expiredAtMillis <= 0L) -> VipExpiry.Unknown

            (expiredAtMillis == PERMANENT_VIP_EXPIRY_MILLIS) -> VipExpiry.Permanent

            else -> VipExpiry.Until(expiredAtMillis)
        }
        return Membership.Vip(vipExpiry)
    }
}
