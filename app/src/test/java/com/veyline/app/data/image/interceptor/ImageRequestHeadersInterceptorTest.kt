package com.veyline.app.data.image.interceptor

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import kotlin.test.assertEquals

/**
 * 验证 [ImageRequestHeadersInterceptor] 对实际 HTTP 请求中图片专用请求头的设置行为。
 *
 * 测试通过 MockWebServer 接收 OkHttpClient 发出的请求，直接检查服务端最终收到的请求头，
 * 不依赖拦截器内部的 Request 构建过程。
 */
class ImageRequestHeadersInterceptorTest {

    /** 验证图片请求自动携带配置的 User-Agent 和 Referer。 */
    @Test
    fun intercept_withoutExistingHeaders_addsImageHeaders() {
        val expectedUserAgent = "Veyline/Test"
        val expectedReferer = "https://example.test/"

        val interceptor = ImageRequestHeadersInterceptor(
            userAgent = expectedUserAgent,
            referer = expectedReferer,
        )

        MockWebServer().use { server ->
            val mockResponse = MockResponse.Builder().build()

            server.enqueue(mockResponse)
            server.start()

            val serverUrl = server.url("/")
            val request = Request.Builder()
                .url(serverUrl)
                .build()
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()
            okHttpClient.newCall(request).execute().use {
                // 同步完成请求并关闭响应
            }

            // 读取 MockWebServer 实际收到的请求，验证两个图片专用请求头
            val recordedRequest = server.takeRequest()
            assertEquals(expectedUserAgent, recordedRequest.headers["User-Agent"])
            assertEquals(expectedReferer, recordedRequest.headers["Referer"])
        }
    }

    /** 验证调用方已有的同名请求头会被图片专用配置覆盖。 */
    @Test
    fun intercept_withExistingHeaders_replacesImageHeaders() {
        val existingUserAgent = "Existing/UserAgent"
        val existingReferer = "https://existing.example/"
        val expectedUserAgent = "Veyline/Test"
        val expectedReferer = "https://example.test/"

        val interceptor = ImageRequestHeadersInterceptor(
            userAgent = expectedUserAgent,
            referer = expectedReferer,
        )

        MockWebServer().use { server ->
            val mockResponse = MockResponse.Builder().build()

            server.enqueue(mockResponse)
            server.start()

            val serverUrl = server.url("/")
            // 在 Request 中预先设置旧值，模拟调用方自行添加 User-Agent 和 Referer
            val request = Request.Builder()
                .url(serverUrl)
                .header("User-Agent", existingUserAgent)
                .header("Referer", existingReferer)
                .build()
            // OkHttp 拦截器会用新值自动覆盖
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()
            okHttpClient.newCall(request).execute().use {
                // 同上
            }

            val recordedRequest = server.takeRequest()
            // 验证旧值已被覆盖，并且没有保留多个同名请求头
            assertEquals(
                listOf(expectedUserAgent),
                recordedRequest.headers.values("User-Agent"),
            )
            assertEquals(
                listOf(expectedReferer),
                recordedRequest.headers.values("Referer"),
            )
        }
    }
}
