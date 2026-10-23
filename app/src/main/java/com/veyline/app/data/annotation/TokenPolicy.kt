package com.veyline.app.data.annotation

/**
 * 声明 Retrofit 接口的 Token 携带策略。
 *
 * 本策略只控制请求是否携带凭证，不保证凭证有效，也不代表用户拥有对应业务权限。
 *
 * @property value 当前接口采用的策略
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TokenPolicy(
    val value: Policy,
) {
    enum class Policy {
        /** 必须携带 Token；本地没有凭证时阻止请求 */
        REQUIRED,

        /** 有 Token 时携带，没有时以匿名身份请求 */
        OPTIONAL,

        /** 禁止携带 Token，即使请求中已有认证头也应移除 */
        FORBIDDEN,
    }
}
