package com.veyline.app.data.network.interceptor

import com.veyline.app.data.annotation.TokenPolicy
import com.veyline.app.data.annotation.TokenPolicy.Policy
import com.veyline.app.data.auth.AuthTokenStore
import com.veyline.app.data.network.exception.MissingAuthTokenException
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import retrofit2.Invocation
import javax.inject.Inject

/**
 * 根据 Retrofit 接口声明的 [TokenPolicy] 设置认证请求头。
 *
 * 未标注策略时默认采用 [Policy.OPTIONAL]。
 * 本拦截器统一管理 Authorization，不保留调用方手动设置的认证头。
 *
 * 仅用于主业务 API 客户端，不用于图片或第三方服务请求。
 */
class AuthInterceptor @Inject constructor(
    private val authTokenStore: AuthTokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val policy = request
            .tag(Invocation::class.java)
            ?.method()
            ?.getAnnotation(TokenPolicy::class.java)
            ?.value
            ?: Policy.OPTIONAL

        val finalRequest: Request = when (policy) {
            Policy.REQUIRED -> {
                val token = readTokenBlocking() ?: throw MissingAuthTokenException()
                request.withToken(token)
            }

            Policy.OPTIONAL -> {
                val token = readTokenBlocking()
                // 没有 token 时也要移除，确保 Authorization 只来自 AuthTokenStore
                if (token != null) request.withToken(token) else request.withoutToken()
            }

            Policy.FORBIDDEN -> request.withoutToken()
        }

        return chain.proceed(finalRequest)
    }

    private fun readTokenBlocking(): String? =
        runBlocking { authTokenStore.getToken() }?.takeIf { it.isNotBlank() }

    private fun Request.withToken(token: String): Request =
        newBuilder().header(AUTHORIZATION_HEADER, "Bearer $token").build()

    private fun Request.withoutToken(): Request =
        newBuilder().removeHeader(AUTHORIZATION_HEADER).build()

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}
