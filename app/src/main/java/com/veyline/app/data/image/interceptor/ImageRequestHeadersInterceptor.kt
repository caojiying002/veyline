package com.veyline.app.data.image.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 为图片网络请求统一设置服务端要求的请求头。
 *
 * 使用 `header`（而非 `addHeader`）设置：请求中已有同名头时替换，不会追加出多个同名头。
 * 具体配置由依赖注入层传入，本拦截器不直接依赖 [com.veyline.app.BuildConfig]。
 *
 * @param userAgent 图片服务要求的 User-Agent，不能为空。
 * @param referer 图片服务用于校验请求来源的 Referer，不能为空。
 */
class ImageRequestHeadersInterceptor(
    private val userAgent: String,
    private val referer: String,
) : Interceptor {

    init {
        require(userAgent.isNotBlank()) {
            "userAgent must not be blank"
        }
        require(referer.isNotBlank()) {
            "referer must not be blank"
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .header(USER_AGENT_HEADER, userAgent)
            .header(REFERER_HEADER, referer)
            .build()

        return chain.proceed(request)
    }

    private companion object {
        const val USER_AGENT_HEADER = "User-Agent"
        const val REFERER_HEADER = "Referer"
    }
}
