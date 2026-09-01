package com.veyline.app.data.network

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.veyline.app.data.network.exception.EmptyResponseBodyException
import com.veyline.app.data.network.exception.MissingDataException
import com.veyline.app.data.network.model.ApiResponse
import com.veyline.app.data.network.model.ApiResult
import com.veyline.app.data.network.model.NoData
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class ApiCallTest {

    /** 验证 HTTP 与业务均成功时返回包含业务数据的 [ApiResult.Success]。 */
    @Test
    fun apiCall_withSuccessfulResponse_returnsSuccess() = runTest {
        val result = apiCall {
            Response.success(
                ApiResponse(
                    code = ApiResponse.CODE_SUCCESS,
                    msg = "success",
                    data = "value",
                ),
            )
        }

        val expected = ApiResult.Success("value")
        assertEquals(expected, result)
    }

    /** 验证表单字段验证失败时返回包含字段错误的 [ApiResult.Failure.Validation]。 */
    @Test
    fun apiCall_withValidationError_returnsValidationFailure() = runTest {
        val fieldErrors = mapOf(
            "username" to "required",
            "password" to "too short",
        )

        val result = apiCall {
            Response.success(
                ApiResponse<String>(
                    code = ApiResponse.CODE_VALIDATION_ERROR,
                    msg = "validation failed",
                    data = null,
                    fieldErrors = fieldErrors,
                ),
            )
        }

        val expected = ApiResult.Failure.Validation(
            code = ApiResponse.CODE_VALIDATION_ERROR,
            message = "validation failed",
            fieldErrors = fieldErrors,
        )

        assertEquals(expected, result)
    }

    /** 验证普通业务失败时返回 [ApiResult.Failure.Business]。 */
    @Test
    fun apiCall_withBusinessError_returnsBusinessFailure() = runTest {
        val result = apiCall {
            Response.success(
                ApiResponse<String>(
                    code = 1000,
                    msg = "business failed",
                    data = null,
                ),
            )
        }

        val expected = ApiResult.Failure.Business(
            code = 1000,
            message = "business failed",
        )

        assertEquals(expected, result)
    }

    /** 验证成功响应缺少必需数据时返回 [ApiResult.Failure.Serialization]。 */
    @Test
    fun apiCall_withMissingData_returnsSerializationFailure() = runTest {
        val result = apiCall {
            Response.success(
                ApiResponse<String>(
                    code = ApiResponse.CODE_SUCCESS,
                    msg = "success",
                    data = null,
                ),
            )
        }

        assertIs<ApiResult.Failure.Serialization>(result)
        assertIs<MissingDataException>(result.exception)
    }

    /** 验证成功的 HTTP 响应不包含响应体时返回 [ApiResult.Failure.Serialization]。 */
    @Test
    fun apiCall_withEmptyResponseBody_returnsSerializationFailure() = runTest {
        val result = apiCall {
            Response.success<ApiResponse<String>>(null)
        }

        assertIs<ApiResult.Failure.Serialization>(result)
        assertIs<EmptyResponseBodyException>(result.exception)
    }

    /** 验证无响应体调用在 HTTP 成功时返回包含 [NoData] 的成功结果。 */
    @Test
    fun apiCallNoContent_withSuccessfulResponse_returnsSuccess() = runTest {
        val result = apiCallNoContent {
            Response.success<Unit>(204, null)
        }

        val expected = ApiResult.Success(NoData)
        assertEquals(expected, result)
    }

    /** 验证非 2XX 响应按状态码转换为 [ApiResult.Failure.Http]。 */
    @Test
    fun apiCall_withUnsuccessfulResponse_returnsHttpFailure() = runTest {
        val result = apiCall {
            Response.error<ApiResponse<String>>(
                404,
                "".toResponseBody(),
            )
        }

        val expected = ApiResult.Failure.Http(
            statusCode = 404,
        )

        assertEquals(expected, result)
    }

    /** 验证 Retrofit 抛出 `HttpException` 时保留状态码和原始异常。 */
    @Test
    fun apiCall_whenThrowingHttpException_returnsHttpFailure() = runTest {
        val exception = HttpException(
            Response.error<String>(
                500,
                "".toResponseBody(),
            ),
        )

        val result = apiCall<String> {
            throw exception
        }

        assertIs<ApiResult.Failure.Http>(result)
        assertEquals(500, result.statusCode)
        assertSame(exception, result.exception)
    }

    /** 验证网络 IO 异常被转换为 [ApiResult.Failure.Network]。 */
    @Test
    fun apiCall_whenThrowingIOException_returnsNetworkFailure() = runTest {
        val exception = IOException("network failed")

        val result = apiCall<String> {
            throw exception
        }

        assertIs<ApiResult.Failure.Network>(result)
        assertSame(exception, result.exception)
    }

    /** 验证 JSON 数据与声明类型不匹配时返回 [ApiResult.Failure.Serialization]。 */
    @Test
    fun apiCall_whenThrowingJsonDataException_returnsSerializationFailure() = runTest {
        val exception = JsonDataException("JSON data does not match the declared type")

        val result = apiCall<String> {
            throw exception
        }

        assertIs<ApiResult.Failure.Serialization>(result)
        assertSame(exception, result.exception)
    }

    /** 验证 JSON 编码格式非法时返回 [ApiResult.Failure.Serialization]。 */
    @Test
    fun apiCall_whenThrowingJsonEncodingException_returnsSerializationFailure() = runTest {
        val exception = JsonEncodingException("Malformed JSON")

        val result = apiCall<String> {
            throw exception
        }

        assertIs<ApiResult.Failure.Serialization>(result)
        assertSame(exception, result.exception)
    }

    /** 验证协程取消不会被转换为普通请求失败。 */
    @Test
    fun apiCall_whenThrowingCancellationException_rethrowsException() = runTest {
        val exception = CancellationException("request cancelled")

        val thrown = assertFailsWith<CancellationException> {
            apiCall<String> {
                throw exception
            }
        }

        assertSame(exception, thrown)
    }

    /** 验证未预期的程序异常不会被网络层吞掉或降级。 */
    @Test
    fun apiCall_whenThrowingUnexpectedException_rethrowsException() = runTest {
        val exception = IllegalStateException("unexpected application error")

        val thrown = assertFailsWith<IllegalStateException> {
            apiCall<String> {
                throw exception
            }
        }

        assertSame(exception, thrown)
    }
}
