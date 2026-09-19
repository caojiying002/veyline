package com.veyline.app.feature.merchant.domain.model

/**
 * 当前用户查看商家联系方式的权限状态。
 *
 * 权限完全由服务端返回的会员状态决定。联系方式为空时，无法区分“用户没有权限”和
 * “商家没有提供”，因此联系方式是否为空不参与权限判断；联系方式只出现在 [Available] 中，
 * 其他状态不携带联系方式。
 *
 * UI 应对每个成员做显式分支处理，不使用 `else` 兜底，这样新增状态时编译器会提示遗漏。
 */
sealed interface MerchantContactAccess {

    /**
     * 可以查看联系方式
     *
     * @property contact 经过清理的联系方式，保证非空
     */
    data class Available(val contact: String) : MerchantContactAccess

    /** 未登录，需要先登录才能查看 */
    data object LoginRequired : MerchantContactAccess

    /** 已登录但不是 VIP，需要升级 VIP 才能查看 */
    data object VipRequired : MerchantContactAccess

    /**
     * 没有可展示的联系方式，也没有可引导用户执行的操作，UI 不展示联系方式区域。
     *
     * 包括两种情况：服务端返回了无法识别的会员状态；用户是 VIP，但商家没有提供有效的
     * 联系方式。
     */
    data object Unavailable : MerchantContactAccess
}
