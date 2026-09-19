package com.veyline.app.feature.merchant.data

import androidx.paging.testing.asSnapshot
import com.veyline.app.data.image.ImageUrlResolver
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.data.network.model.ApiResponseDto
import com.veyline.app.data.network.model.PagedDataDto
import com.veyline.app.data.network.result.ApiResult
import com.veyline.app.feature.merchant.data.mapper.MerchantDetailMapper
import com.veyline.app.feature.merchant.data.mapper.MerchantSummaryMapper
import com.veyline.app.feature.merchant.data.remote.MerchantApiService
import com.veyline.app.feature.merchant.data.remote.model.MerchantDetailDto
import com.veyline.app.feature.merchant.data.remote.model.MerchantProvinceDto
import com.veyline.app.feature.merchant.data.remote.model.MerchantSummaryDto
import com.veyline.app.feature.merchant.domain.model.MerchantContactAccess
import com.veyline.app.feature.merchant.domain.model.MerchantDetail
import com.veyline.app.feature.merchant.domain.model.MerchantProvince
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
import kotlin.time.Duration.Companion.milliseconds

/** 验证 [MerchantRepository] 在商家分页、省份列表与商家详情三类数据上的行为。 */
class MerchantRepositoryTest {

    private val imageUrlResolver = ImageUrlResolver(
        baseUrl = "https://example.test/images/",
    )

    private val merchantSummaryMapper = MerchantSummaryMapper(
        imageUrlResolver = imageUrlResolver,
    )

    private val merchantDetailMapper = MerchantDetailMapper(
        imageUrlResolver = imageUrlResolver,
    )

    /** 验证商家分页首次加载使用固定页大小，并规范化地区筛选代码。 */
    @Test
    fun getMerchants_onFirstLoad_usesPagingConfigAndNormalizedProvinceCode() = runTest {
        val merchantDto = MerchantSummaryDto(
            id = "merchant-a",
            name = "商家甲",
            cityCode = "province-a",
            intro = "商家简介",
            coverPicture = null,
        )

        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchants(
                page = 1,
                perPage = 12,
                cityCode = "province-a",
            )
        } returns Response.success(
            ApiResponseDto(
                code = ApiResponseDto.CODE_SUCCESS,
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
        val repository = createRepository(apiService)

        val merchants = repository
            .getMerchants(provinceCode = "  province-a  ") // 验证 Repository 会清理地区代码两侧的空白
            .asSnapshot() // 收集 PagingData 当前加载结果，并转换为便于断言的普通 List

        val expected = listOf(
            MerchantSummary(
                id = "merchant-a",
                name = "商家甲",
                provinceCode = "province-a",
                intro = "商家简介",
                coverImageUrl = null,
            ),
        )
        assertEquals(expected, merchants)
    }

    /** 验证成功加载后复用进程内缓存，不重复请求接口。 */
    @Test
    fun getMerchantProvinces_afterSuccessfulResponse_usesCache() = runTest {
        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchantProvinces()
        } returns Response.success(
            ApiResponseDto(
                code = ApiResponseDto.CODE_SUCCESS,
                msg = "success",
                data = listOf(
                    MerchantProvinceDto(
                        code = "code-a",
                        name = "省份甲",
                    ),
                ),
            ),
        )
        val repository = createRepository(apiService)

        val expected = ApiResult.Success(
            listOf(
                MerchantProvince(
                    code = "code-a",
                    name = "省份甲",
                ),
            ),
        )
        val firstResult = repository.getMerchantProvinces()
        val secondResult = repository.getMerchantProvinces()

        assertEquals(expected, firstResult)
        assertEquals(expected, secondResult)
        coVerify(exactly = 1) {
            apiService.getMerchantProvinces()
        }
    }

    /** 验证成功返回空列表时不写入缓存，使后续调用能够重新请求。 */
    @Test
    fun getMerchantProvinces_afterEmptySuccessfulResponse_requestsAgain() = runTest {
        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchantProvinces()
        } returns Response.success(
            ApiResponseDto(
                code = ApiResponseDto.CODE_SUCCESS,
                msg = "success",
                data = emptyList(),
            ),
        )
        val repository = createRepository(apiService)

        val expected = ApiResult.Success(emptyList<MerchantProvince>())
        val firstResult = repository.getMerchantProvinces()
        val secondResult = repository.getMerchantProvinces()

        assertEquals(expected, firstResult)
        assertEquals(expected, secondResult)
        coVerify(exactly = 2) {
            apiService.getMerchantProvinces()
        }
    }

    /** 验证地区数据全部无效时不写入缓存，后续调用仍会重新请求。 */
    @Test
    fun getMerchantProvinces_afterInvalidData_requestsAgain() = runTest {
        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchantProvinces()
        } returns Response.success(
            ApiResponseDto(
                code = ApiResponseDto.CODE_SUCCESS,
                msg = "success",
                data = listOf(
                    MerchantProvinceDto(
                        code = null,
                        name = "省份甲",
                    ),
                    MerchantProvinceDto(
                        code = "code-b",
                        name = "   ",
                    ),
                ),
            ),
        )
        val repository = createRepository(apiService)

        val firstResult = repository.getMerchantProvinces()
        val secondResult = repository.getMerchantProvinces()

        assertIs<ApiResult.Failure.Serialization>(firstResult)
        assertIs<InvalidApiDataException>(firstResult.exception)

        assertIs<ApiResult.Failure.Serialization>(secondResult)
        assertIs<InvalidApiDataException>(secondResult.exception)

        // 两次调用都重新请求接口，说明转换失败的结果没有写入缓存
        coVerify(exactly = 2) {
            apiService.getMerchantProvinces()
        }
    }

    /** 验证业务失败不写入缓存，后续调用仍会重新请求接口。 */
    @Test
    fun getMerchantProvinces_afterBusinessError_requestsAgain() = runTest {
        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchantProvinces()
        } returns Response.success(
            ApiResponseDto(
                code = 1000,
                msg = "business failed",
                data = null,
            ),
        )
        val repository = createRepository(apiService)

        val expected = ApiResult.Failure.Business(
            code = 1000,
            message = "business failed",
        )
        val firstResult = repository.getMerchantProvinces()
        val secondResult = repository.getMerchantProvinces()

        assertEquals(expected, firstResult)
        assertEquals(expected, secondResult)
        coVerify(exactly = 2) {
            apiService.getMerchantProvinces()
        }
    }

    /** 验证多个并发首次调用共享同一次接口请求，并取得相同的成功数据。 */
    @Test
    fun getMerchantProvinces_withConcurrentInitialCalls_requestsOnce() = runTest {
        val response = Response.success(
            ApiResponseDto(
                code = ApiResponseDto.CODE_SUCCESS,
                msg = "success",
                data = listOf(
                    MerchantProvinceDto(
                        code = "code-a",
                        name = "省份甲",
                    ),
                ),
            ),
        )

        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchantProvinces()
        } coAnswers {
            // 让第一个调用保持挂起，确保第二个调用会在缓存写入前尝试进入 Repository
            delay(100.milliseconds)
            response
        }
        val repository = createRepository(apiService)

        val expected = ApiResult.Success(
            listOf(
                MerchantProvince(
                    code = "code-a",
                    name = "省份甲",
                ),
            ),
        )
        val results: List<ApiResult<List<MerchantProvince>>> = listOf(
            async { repository.getMerchantProvinces() },
            async { repository.getMerchantProvinces() },
        ).awaitAll()

        assertEquals(listOf(expected, expected), results)
        coVerify(exactly = 1) {
            apiService.getMerchantProvinces()
        }
    }

    /** 验证详情接口成功时返回经过清洗和图片地址解析的领域模型。 */
    @Test
    fun getMerchantDetail_withSuccessfulResponse_returnsMappedDetail() = runTest {
        val detailDto = MerchantDetailDto(
            id = " merchant-a ",
            name = " 商家甲 ",
            cityCode = " province-a ",
            picture = "first.jpg, second.jpg",
            desc = " 商家详情 ",
            vipProfileStatus = 1,
            contact = " 联系方式 ",
        )
        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchantDetail(
                merchantId = "merchant-a",
            )
        } returns Response.success(
            ApiResponseDto(
                code = ApiResponseDto.CODE_SUCCESS,
                msg = "success",
                data = detailDto,
            ),
        )

        val expected = ApiResult.Success(
            MerchantDetail(
                id = "merchant-a",
                name = "商家甲",
                provinceCode = "province-a",
                imageUrls = listOf(
                    "https://example.test/images/first.jpg",
                    "https://example.test/images/second.jpg",
                ),
                description = "商家详情",
                contactAccess = MerchantContactAccess.Available("联系方式"),
            ),
        )

        val repository = createRepository(apiService)
        val result = repository.getMerchantDetail(
            merchantId = "merchant-a",
        )
        assertEquals(expected, result)
    }

    /** 验证详情响应缺少必要字段时返回序列化失败。 */
    @Test
    fun getMerchantDetail_withInvalidData_returnsSerializationFailure() = runTest {
        val invalidDetailDto = MerchantDetailDto(
            id = null,
            name = "商家甲",
            cityCode = "province-a",
            picture = null,
            desc = null,
            vipProfileStatus = 1,
            contact = null,
        )
        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchantDetail(
                merchantId = "merchant-a",
            )
        } returns Response.success(
            ApiResponseDto(
                code = ApiResponseDto.CODE_SUCCESS,
                msg = "success",
                data = invalidDetailDto,
            ),
        )

        val repository = createRepository(apiService)
        // ID为 `null`，预期返回序列化失败
        val result = repository.getMerchantDetail(
            merchantId = "merchant-a",
        )
        assertIs<ApiResult.Failure.Serialization>(result)
        assertIs<InvalidApiDataException>(result.exception)
    }

    /** 验证详情接口返回业务失败时保留原始失败信息。 */
    @Test
    fun getMerchantDetail_withBusinessFailure_returnsBusinessFailure() = runTest {
        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchantDetail(
                merchantId = "merchant-a",
            )
        } returns Response.success(
            ApiResponseDto(
                code = 1000,
                msg = "business failed",
                data = null,
            ),
        )

        val expected = ApiResult.Failure.Business(
            code = 1000,
            message = "business failed",
        )
        val repository = createRepository(apiService)
        val result = repository.getMerchantDetail(
            merchantId = "merchant-a",
        )
        assertEquals(expected, result)
    }

    private fun createRepository(
        apiService: MerchantApiService,
    ): MerchantRepository =
        MerchantRepository(
            apiService = apiService,
            merchantSummaryMapper = merchantSummaryMapper,
            merchantDetailMapper = merchantDetailMapper,
        )
}
