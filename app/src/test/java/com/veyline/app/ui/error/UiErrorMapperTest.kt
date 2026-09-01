package com.veyline.app.ui.error

import com.veyline.app.data.network.model.ApiResult
import com.veyline.app.data.network.model.ApiResponse
import java.io.IOException
import kotlin.test.assertSame
import org.junit.Test

/**
 * 验证网络调用失败到通用 UI 错误的默认映射规则。
 *
 * 具体业务错误码不属于本测试范围，由相应 feature 的错误映射测试负责。
 */
class UiErrorMapperTest {

    /** 验证网络连接失败被转换为连接类 UI 错误。 */
    @Test
    fun toUiError_withNetworkFailure_returnsConnection() {
        val failure = ApiResult.Failure.Network(
            IOException("network failed"),
        )

        val result = failure.toUiError()

        assertSame(UiError.Connection, result)
    }

    /** 验证未由具体业务处理的普通业务失败被转换为技术类 UI 错误。 */
    @Test
    fun toUiError_withBusinessFailure_returnsTechnical() {
        val failure = ApiResult.Failure.Business(
            code = 1000,
            message = "business failed",
        )

        val result = failure.toUiError()

        assertSame(UiError.Technical, result)
    }

    /** 验证未由表单页面单独处理的字段验证失败被转换为技术类 UI 错误。 */
    @Test
    fun toUiError_withValidationFailure_returnsTechnical() {
        val failure = ApiResult.Failure.Validation(
            code = ApiResponse.CODE_VALIDATION_ERROR,
            message = "validation failed",
            fieldErrors = mapOf("username" to "required"),
        )

        val result = failure.toUiError()

        assertSame(UiError.Technical, result)
    }

    /** 验证 HTTP 状态失败被转换为技术类 UI 错误。 */
    @Test
    fun toUiError_withHttpFailure_returnsTechnical() {
        val failure = ApiResult.Failure.Http(
            statusCode = 503,
            exception = null,
        )

        val result = failure.toUiError()

        assertSame(UiError.Technical, result)
    }

    /** 验证响应内容无法解析时被转换为技术类 UI 错误。 */
    @Test
    fun toUiError_withSerializationFailure_returnsTechnical() {
        val failure = ApiResult.Failure.Serialization(
            exception = IllegalStateException("serialization failed"),
        )

        val result = failure.toUiError()

        assertSame(UiError.Technical, result)
    }

    /** 验证允许降级处理的未预期失败被转换为技术类 UI 错误。 */
    @Test
    fun toUiError_withUnexpectedFailure_returnsTechnical() {
        val failure = ApiResult.Failure.Unexpected(
            exception = IllegalStateException("unexpected failure"),
        )

        val result = failure.toUiError()

        assertSame(UiError.Technical, result)
    }
}
