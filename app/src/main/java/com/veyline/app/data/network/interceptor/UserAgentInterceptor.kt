package com.veyline.app.data.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 为所有经过当前 OkHttpClient 的请求统一设置 User-Agent 请求头。
 *
 * 使用 [okhttp3.Request.Builder.header] 会替换请求中已有的同名请求头，避免调用方自行设置
 * User-Agent 后产生多个值。具体值由网络层装配代码传入，本拦截器不依赖构建配置。
 *
 * @param userAgent 写入请求头的 User-Agent 值，不能为空。
 */
class UserAgentInterceptor(
    private val userAgent: String,
) : Interceptor {

    init {
        require(userAgent.isNotBlank()) {
            "userAgent must not be blank"
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .header(USER_AGENT_HEADER, userAgent)
            .build()

        return chain.proceed(request)
    }

    private companion object {
        const val USER_AGENT_HEADER = "User-Agent"
    }
}
