package com.veyline.app.data.network.interceptor

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import kotlin.test.assertEquals

/**
 * 验证 [UserAgentInterceptor] 对实际 HTTP 请求中 User-Agent 请求头的统一设置行为。
 *
 * 测试通过 MockWebServer 接收 OkHttpClient 发出的请求，直接检查服务端最终收到的请求头，
 * 不依赖拦截器内部的 Request 构建过程。
 */
class UserAgentInterceptorTest {

    /** 验证原请求未设置 User-Agent 时添加配置的请求头值。 */
    @Test
    fun intercept_withoutUserAgent_addsConfiguredValue() {
        val expectedUserAgent = "Veyline/Test"

        MockWebServer().use { server ->
            server.enqueue(MockResponse.Builder().build())
            server.start()

            // 使用 MockWebServer 提供的 URL，确保请求发送到当前测试服务器
            val mockWebServerUrl = server.url("/")
            // 使用带拦截器的真实 OkHttpClient 向本机测试服务器发起请求
            val client = OkHttpClient.Builder()
                .addInterceptor(UserAgentInterceptor(expectedUserAgent))
                .build()
            val request = Request.Builder()
                .url(mockWebServerUrl)
                .build()
            client.newCall(request).execute().use {
                // 同步完成请求并关闭响应后，MockWebServer 才能取得最终收到的请求
            }

            // 读取 MockWebServer 实际接收的请求，验证服务端最终看到的请求头
            val recordedRequest = server.takeRequest()
            assertEquals(expectedUserAgent, recordedRequest.headers["User-Agent"])
        }
    }

    /** 验证原请求已设置 User-Agent 时覆盖旧值，且不会保留多个同名请求头。 */
    @Test
    fun intercept_withUserAgent_replacesExistingValue() {
        val existingUserAgent = "Existing/Test"
        val expectedUserAgent = "Veyline/Test"

        MockWebServer().use { server ->
            server.enqueue(MockResponse.Builder().build())
            server.start()

            val mockWebServerUrl = server.url("/")
            val client = OkHttpClient.Builder()
                .addInterceptor(UserAgentInterceptor(expectedUserAgent))
                .build()
            // 在原请求中预先设置旧值，模拟调用方自行添加 User-Agent
            val request = Request.Builder()
                .url(mockWebServerUrl)
                .header("User-Agent", existingUserAgent)
                .build()
            client.newCall(request).execute().use {
                // 同上
            }

            val recordedRequest = server.takeRequest()
            // 同时验证旧值已被覆盖，并且没有保留多个同名请求头
            assertEquals(
                listOf(expectedUserAgent),
                recordedRequest.headers.values("User-Agent"),
            )
        }
    }
}
