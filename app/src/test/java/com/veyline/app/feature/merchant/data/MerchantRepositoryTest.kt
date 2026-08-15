package com.veyline.app.feature.merchant.data

import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.data.network.model.ApiResponse
import com.veyline.app.data.network.model.ApiResult
import com.veyline.app.feature.merchant.data.remote.MerchantApiService
import com.veyline.app.feature.merchant.data.remote.model.MerchantCityDto
import com.veyline.app.feature.merchant.domain.model.MerchantCity
import kotlinx.coroutines.test.runTest
import org.junit.Test
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * 验证 [MerchantRepository] 对商家城市接口结果的转换边界。
 *
 * Mapper 的具体字段校验由其独立测试覆盖；本测试只关注网络调用结果如何转换并暴露给上层。
 */
class MerchantRepositoryTest {

    /** 验证接口成功时返回经过 Mapper 转换的城市领域模型。 */
    @Test
    fun `getMerchantCities with successful response returns mapped cities`() = runTest {
        val apiService = object : MerchantApiService {
            override suspend fun getMerchantCities():
                    Response<ApiResponse<List<MerchantCityDto>>> =
                Response.success(
                    ApiResponse(
                        code = ApiResponse.CODE_SUCCESS,
                        msg = "success",
                        data = listOf(
                            MerchantCityDto(
                                code = "code-a",
                                name = "城市甲",
                            ),
                        ),
                    ),
                )
        }
        val repository = MerchantRepository(apiService)
        val expected = ApiResult.Success(
            listOf(
                MerchantCity(
                    code = "code-a",
                    name = "城市甲",
                ),
            ),
        )

        val result = repository.getMerchantCities()

        assertEquals(expected, result)
    }

    /** 验证接口返回空列表时仍返回成功结果，由上层决定如何展示空状态。 */
    @Test
    fun `getMerchantCities with empty response returns empty Success`() = runTest {
        val apiService = object : MerchantApiService {
            override suspend fun getMerchantCities():
                    Response<ApiResponse<List<MerchantCityDto>>> =
                Response.success(
                    ApiResponse(
                        code = ApiResponse.CODE_SUCCESS,
                        msg = "success",
                        data = emptyList(),
                    ),
                )
        }
        val repository = MerchantRepository(apiService)
        val expected = ApiResult.Success(emptyList<MerchantCity>())

        val result = repository.getMerchantCities()

        assertEquals(expected, result)
    }

    /** 验证接口成功但城市字段无效时返回序列化失败。 */
    @Test
    fun `getMerchantCities with invalid city returns Serialization failure`() = runTest {
        val apiService = object : MerchantApiService {
            override suspend fun getMerchantCities():
                    Response<ApiResponse<List<MerchantCityDto>>> =
                Response.success(
                    ApiResponse(
                        code = ApiResponse.CODE_SUCCESS,
                        msg = "success",
                        data = listOf(
                            MerchantCityDto(
                                code = null,
                                name = "城市甲",
                            ),
                        ),
                    ),
                )
        }
        val repository = MerchantRepository(apiService)

        val result = repository.getMerchantCities()

        assertIs<ApiResult.Failure.Serialization>(result)
        assertIs<InvalidApiDataException>(result.exception)
        assertEquals(
            "Merchant city at index 0 has a missing or blank code",
            result.exception.message,
        )
    }

    /** 验证接口返回业务失败时 Repository 不改写失败结果。 */
    @Test
    fun `getMerchantCities with business error returns Business failure`() = runTest {
        val apiService = object : MerchantApiService {
            override suspend fun getMerchantCities():
                    Response<ApiResponse<List<MerchantCityDto>>> =
                Response.success(
                    ApiResponse(
                        code = 1000,
                        msg = "business failed",
                        data = null,
                    ),
                )
        }
        val repository = MerchantRepository(apiService)
        val expected = ApiResult.Failure.Business(
            code = 1000,
            message = "business failed",
        )

        val result = repository.getMerchantCities()

        assertEquals(expected, result)
    }
}
