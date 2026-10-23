package com.veyline.app.data.network.exception

import java.io.IOException

/**
 * `AuthInterceptor` 判定请求需要 token 但本地没有登录凭证，因此没有发出请求。
 *
 * 必须继承 [IOException]，不能跟同目录下其他异常一样继承 [IllegalStateException]：
 * [okhttp3.Interceptor.intercept] 只能返回 [okhttp3.Response] 或抛出 [IOException]，
 * 抛出其他类型的异常会在 OkHttp 的请求调度线程上造成难以排查的崩溃。
 *
 * 本异常表示客户端本地判定的前置条件不满足，不代表请求真的发生过网络交互或收到过 HTTP
 * 响应。
 */
class MissingAuthTokenException(
    message: String = "Request requires an authentication token, but none is available",
    cause: Throwable? = null,
) : IOException(message, cause)
