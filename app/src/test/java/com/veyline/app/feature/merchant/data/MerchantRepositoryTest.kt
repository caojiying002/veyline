package com.veyline.app.feature.merchant.data

import androidx.paging.testing.asSnapshot
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.data.network.model.ApiResponse
import com.veyline.app.data.network.model.ApiResult
import com.veyline.app.data.network.model.PagedDataDto
import com.veyline.app.feature.merchant.data.remote.MerchantApiService
import com.veyline.app.feature.merchant.data.remote.model.MerchantCityDto
import com.veyline.app.feature.merchant.data.remote.model.MerchantSummaryDto
import com.veyline.app.feature.merchant.domain.model.MerchantCity
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * 验证 [MerchantRepository] 对商家分页数据和城市筛选数据的组织边界。
 *
 * 商家分页测试关注 Pager 的首次加载配置、筛选参数规范化以及向上层暴露的领域模型；城市
 * 列表测试关注网络结果转换，以及成功、空数据和失败结果对应的进程内缓存行为。Mapper 和
 * PagingSource 内部的具体字段校验、分页键计算与去重规则由各自的独立测试覆盖。
 */
class MerchantRepositoryTest {

    /** 验证商家分页首次加载使用固定页大小，并规范化城市筛选代码。 */
    @Test
    fun `getMerchants first load uses paging config and normalized city code`() = runTest {
        val merchantDto = MerchantSummaryDto(
            id = "merchant-a",
            name = "商家甲",
            cityCode = "city-a",
            intro = "商家简介",
            coverPicture = null,
        )

        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchants(
                page = 1,
                perPage = 12,
                cityCode = "city-a",
            )
        } returns Response.success(
            ApiResponse(
                code = ApiResponse.CODE_SUCCESS,
                msg = "success",
                data = PagedDataDto(
                    records = listOf(merchantDto),
                    total = 1,
                    size = 12,
                    current = 1,
                    pages = 1,
                ),
            ),
        )

        val repository = MerchantRepository(apiService)
        val merchants = repository
            .getMerchants(cityCode = "  city-a  ") // 验证 Repository 会清理城市代码两侧的空白
            .asSnapshot() // 收集 PagingData 当前加载结果，并转换为便于断言的普通 List

        val expected = listOf(
            MerchantSummary(
                id = "merchant-a",
                name = "商家甲",
                cityCode = "city-a",
                intro = "商家简介",
                coverImagePath = null,
            ),
        )
        assertEquals(expected, merchants)
    }

    /** 验证成功加载后复用进程内缓存，不重复请求接口。 */
    @Test
    fun `getMerchantCities after successful response uses cache`() = runTest {
        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchantCities()
        } returns Response.success(
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
        coVerify(exactly = 1) {
            apiService.getMerchantCities()
        }
    }

    /** 验证成功返回空列表时不写入缓存，使后续调用能够重新请求。 */
    @Test
    fun `getMerchantCities after empty successful response requests again`() = runTest {
        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchantCities()
        } returns Response.success(
            ApiResponse(
                code = ApiResponse.CODE_SUCCESS,
                msg = "success",
                data = emptyList(),
            ),
        )
        val repository = MerchantRepository(apiService)

        val expected = ApiResult.Success(emptyList<MerchantCity>())
        val firstResult = repository.getMerchantCities()
        val secondResult = repository.getMerchantCities()

        assertEquals(expected, firstResult)
        assertEquals(expected, secondResult)
        coVerify(exactly = 2) {
            apiService.getMerchantCities()
        }
    }

    /** 验证城市数据全部无效时不写入缓存，后续调用仍会重新请求。 */
    @Test
    fun `getMerchantCities after invalid data requests again`() = runTest {
        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchantCities()
        } returns Response.success(
            ApiResponse(
                code = ApiResponse.CODE_SUCCESS,
                msg = "success",
                data = listOf(
                    MerchantCityDto(
                        code = null,
                        name = "城市甲",
                    ),
                    MerchantCityDto(
                        code = "code-b",
                        name = "   ",
                    ),
                ),
            ),
        )
        val repository = MerchantRepository(apiService)

        val firstResult = repository.getMerchantCities()
        val secondResult = repository.getMerchantCities()

        assertIs<ApiResult.Failure.Serialization>(firstResult)
        assertIs<InvalidApiDataException>(firstResult.exception)

        assertIs<ApiResult.Failure.Serialization>(secondResult)
        assertIs<InvalidApiDataException>(secondResult.exception)

        // 两次调用都重新请求接口，说明转换失败的结果没有写入缓存
        coVerify(exactly = 2) {
            apiService.getMerchantCities()
        }
    }

    /** 验证业务失败不写入缓存，后续调用仍会重新请求接口。 */
    @Test
    fun `getMerchantCities after business error requests again`() = runTest {
        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchantCities()
        } returns Response.success(
            ApiResponse(
                code = 1000,
                msg = "business failed",
                data = null,
            ),
        )
        val repository = MerchantRepository(apiService)

        val expected = ApiResult.Failure.Business(
            code = 1000,
            message = "business failed",
        )
        val firstResult = repository.getMerchantCities()
        val secondResult = repository.getMerchantCities()

        assertEquals(expected, firstResult)
        assertEquals(expected, secondResult)
        coVerify(exactly = 2) {
            apiService.getMerchantCities()
        }
    }

    /** 验证多个并发首次调用共享同一次接口请求，并取得相同的成功数据。 */
    @Test
    fun `getMerchantCities with concurrent initial calls requests once`() = runTest {
        val response = Response.success(
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

        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchantCities()
        } coAnswers {
            // 让第一个调用保持挂起，确保第二个调用会在缓存写入前尝试进入 Repository
            delay(100)
            response
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
        coVerify(exactly = 1) {
            apiService.getMerchantCities()
        }
    }
}
