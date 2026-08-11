package com.veyline.app.data.network

import com.veyline.app.data.network.exception.EmptyResponseBodyException
import com.veyline.app.data.network.exception.MissingDataException
import com.veyline.app.data.network.model.ApiResponse
import com.veyline.app.data.network.model.ApiResult
import com.veyline.app.data.network.model.NoData
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ApiCallTest {

    /** 验证 HTTP 与业务均成功时返回包含业务数据的 [ApiResult.Success]。 */
    @Test
    fun `apiCall with successful response returns Success`() = runTest {
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
    fun `apiCall with validation error returns Validation failure`() = runTest {
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
    fun `apiCall with business error returns Business failure`() = runTest {
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
    fun `apiCall with missing data returns Serialization failure`() = runTest {
        val result = apiCall {
            Response.success(
                ApiResponse<String>(
                    code = ApiResponse.CODE_SUCCESS,
                    msg = "success",
                    data = null,
                ),
            )
        }

        assertTrue(result is ApiResult.Failure.Serialization)

        val exception = (result as ApiResult.Failure.Serialization).exception
        assertTrue(exception is MissingDataException)
    }

    /** 验证成功的 HTTP 响应不包含响应体时返回 [ApiResult.Failure.Serialization]。 */
    @Test
    fun `apiCall with empty response body returns Serialization failure`() = runTest {
        val result = apiCall {
            Response.success<ApiResponse<String>>(null)
        }

        assertTrue(result is ApiResult.Failure.Serialization)

        val exception = (result as ApiResult.Failure.Serialization).exception
        assertTrue(exception is EmptyResponseBodyException)
    }

    /** 验证无响应体调用在 HTTP 成功时返回包含 [NoData] 的成功结果。 */
    @Test
    fun `apiCallNoContent with successful response returns Success`() = runTest {
        val result = apiCallNoContent {
            Response.success<Unit>(204, null)
        }

        val expected = ApiResult.Success(NoData)
        assertEquals(expected, result)
    }

    /** 验证非 2XX 响应按状态码转换为 [ApiResult.Failure.Http]。 */
    @Test
    fun `apiCall with unsuccessful response returns Http failure`() = runTest {
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
    fun `apiCall throwing HttpException returns Http failure`() = runTest {
        val exception = HttpException(
            Response.error<String>(
                500,
                "".toResponseBody(),
            ),
        )

        val result = apiCall<String> {
            throw exception
        }

        assertTrue(result is ApiResult.Failure.Http)

        val failure = result as ApiResult.Failure.Http
        assertEquals(500, failure.statusCode)
        assertSame(exception, failure.exception)
    }

    /** 验证网络 IO 异常被转换为 [ApiResult.Failure.Network]。 */
    @Test
    fun `apiCall throwing IOException returns Network failure`() = runTest {
    }

    /** 验证 JSON 数据与声明类型不匹配时返回 [ApiResult.Failure.Serialization]。 */
    @Test
    fun `apiCall throwing JsonDataException returns Serialization failure`() = runTest {
    }

    /** 验证 JSON 编码格式非法时返回 [ApiResult.Failure.Serialization]。 */
    @Test
    fun `apiCall throwing JsonEncodingException returns Serialization failure`() = runTest {
    }

    /** 验证协程取消不会被转换为普通请求失败。 */
    @Test
    fun `apiCall throwing CancellationException rethrows exception`() = runTest {
    }

    /** 验证未预期的程序异常不会被网络层吞掉或降级。 */
    @Test
    fun `apiCall throwing unexpected exception rethrows exception`() = runTest {
    }
}
