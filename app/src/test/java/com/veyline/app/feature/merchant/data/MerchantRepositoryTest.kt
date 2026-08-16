package com.veyline.app.feature.merchant.data

import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.data.network.model.ApiResponse
import com.veyline.app.data.network.model.ApiResult
import com.veyline.app.feature.merchant.data.remote.MerchantApiService
import com.veyline.app.feature.merchant.data.remote.model.MerchantCityDto
import com.veyline.app.feature.merchant.domain.model.MerchantCity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * 验证 [MerchantRepository] 对商家城市接口结果的转换边界。
 *
 * Mapper 的具体字段校验由其独立测试覆盖；本测试关注网络调用结果如何转换并暴露给上层，
 * 以及成功、空数据和失败结果对应的进程内缓存行为。
 */
class MerchantRepositoryTest {

    /** 验证成功加载后复用进程内缓存，不重复请求接口。 */
    @Test
    fun `getMerchantCities after successful response uses cache`() = runTest {
        var requestCount = 0
        val apiService = object : MerchantApiService {
            override suspend fun getMerchantCities():
                    Response<ApiResponse<List<MerchantCityDto>>> {
                requestCount++

                return Response.success(
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

        val firstResult = repository.getMerchantCities()
        val secondResult = repository.getMerchantCities()

        assertEquals(expected, firstResult)
        assertEquals(expected, secondResult)
        assertEquals(1, requestCount)
    }

    /** 验证成功返回空列表时不写入缓存，使后续调用能够重新请求。 */
    @Test
    fun `getMerchantCities after empty successful response requests again`() = runTest {
        var requestCount = 0
        val apiService = object : MerchantApiService {
            override suspend fun getMerchantCities():
                    Response<ApiResponse<List<MerchantCityDto>>> {
                requestCount++

                return Response.success(
                    ApiResponse(
                        code = ApiResponse.CODE_SUCCESS,
                        msg = "success",
                        data = emptyList(),
                    ),
                )
            }
        }
        val repository = MerchantRepository(apiService)
        val expected = ApiResult.Success(emptyList<MerchantCity>())

        val firstResult = repository.getMerchantCities()
        val secondResult = repository.getMerchantCities()

        assertEquals(expected, firstResult)
        assertEquals(expected, secondResult)
        assertEquals(2, requestCount)
    }

    /** 验证城市数据转换失败时不写入缓存，后续调用仍会重新请求。 */
    @Test
    fun `getMerchantCities after invalid city requests again`() = runTest {
        var requestCount = 0
        val apiService = object : MerchantApiService {
            override suspend fun getMerchantCities():
                    Response<ApiResponse<List<MerchantCityDto>>> {
                requestCount++

                return Response.success(
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
        }
        val repository = MerchantRepository(apiService)

        val firstResult = repository.getMerchantCities()
        val secondResult = repository.getMerchantCities()

        assertIs<ApiResult.Failure.Serialization>(firstResult)
        assertIs<InvalidApiDataException>(firstResult.exception)
        assertEquals(
            "Merchant city at index 0 has a missing or blank code",
            firstResult.exception.message,
        )

        assertIs<ApiResult.Failure.Serialization>(secondResult)
        assertIs<InvalidApiDataException>(secondResult.exception)
        assertEquals(
            "Merchant city at index 0 has a missing or blank code",
            secondResult.exception.message,
        )

        assertEquals(2, requestCount)
    }

    /** 验证业务失败不写入缓存，后续调用仍会重新请求接口。 */
    @Test
    fun `getMerchantCities after business error requests again`() = runTest {
        var requestCount = 0
        val apiService = object : MerchantApiService {
            override suspend fun getMerchantCities():
                    Response<ApiResponse<List<MerchantCityDto>>> {
                requestCount++

                return Response.success(
                    ApiResponse(
                        code = 1000,
                        msg = "business failed",
                        data = null,
                    ),
                )
            }
        }
        val repository = MerchantRepository(apiService)
        val expected = ApiResult.Failure.Business(
            code = 1000,
            message = "business failed",
        )

        val firstResult = repository.getMerchantCities()
        val secondResult = repository.getMerchantCities()

        assertEquals(expected, firstResult)
        assertEquals(expected, secondResult)
        assertEquals(2, requestCount)
    }

    /** 验证多个并发首次调用共享同一次接口请求，并取得相同的成功数据。 */
    @Test
    fun `getMerchantCities with concurrent initial calls requests once`() = runTest {
        var requestCount = 0
        val apiService = object : MerchantApiService {
            override suspend fun getMerchantCities():
                    Response<ApiResponse<List<MerchantCityDto>>> {
                requestCount++

                // 防止第一次调用过早完成，确保第二个调用会在 Mutex 仍被持有时启动。
                delay(100)

                return Response.success(
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

        val results: List<ApiResult<List<MerchantCity>>> = listOf(
            async { repository.getMerchantCities() },
            async { repository.getMerchantCities() },
        ).awaitAll()

        assertEquals(listOf(expected, expected), results)
        assertEquals(1, requestCount)
    }
}
