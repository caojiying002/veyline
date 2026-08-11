package com.veyline.app.data.network

import com.veyline.app.data.network.model.ApiResponse
import com.veyline.app.data.network.model.ApiResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
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
                )
            )
        }

        assertEquals(
            ApiResult.Success("value"),
            result,
        )
    }
}
