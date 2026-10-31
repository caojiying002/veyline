package com.veyline.app.feature.profile.domain.model

/**
 * 当前用户的会员身份。
 *
 * 区分普通用户与 VIP 会员，VIP 的有效期由 [VipExpiry] 描述。
 * 由 Mapper 根据服务端资料转换，不依据本地时间自行推导会员身份。
 *
 * 本类型用于个人中心展示，不作为详情访问或购买操作的授权依据，
 * 实际权限仍由服务端判断。
 */
sealed interface Membership {

    /** 普通用户 */
    data object Regular : Membership

    /** VIP 会员，到期信息由 [VipExpiry] 描述 */
    data class Vip(val expiry: VipExpiry) : Membership
}

/**
 * VIP 会员的有效期信息。
 *
 * 区分永久会员、指定时间到期和有效期未知三种情况。
 * 服务端的特殊时间戳由 Mapper 转换为领域状态，UI 不直接判断协议魔数。
 *
 * [Unknown] 仅表示有效期信息不可用，不代表用户不是 VIP；
 * 具体日期的格式化与展示时区由 UI 层决定。
 */
sealed interface VipExpiry {

    /** 永久 VIP 会员 */
    data object Permanent : VipExpiry

    /**
     * VIP 会员有效期截止到某个时间
     *
     * @property expiresAtMillis 到期时间，以 UTC 毫秒时间戳表示
     */
    data class Until(val expiresAtMillis: Long) : VipExpiry

    /** 服务端没有返回到期时间，或者值无法识别 */
    data object Unknown : VipExpiry
}
