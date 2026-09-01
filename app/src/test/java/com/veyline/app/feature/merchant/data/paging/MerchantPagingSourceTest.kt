package com.veyline.app.feature.merchant.data.paging

import androidx.paging.PagingSource
import com.veyline.app.data.image.ImageUrlResolver
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.data.network.model.ApiResponse
import com.veyline.app.data.network.model.PagedDataDto
import com.veyline.app.feature.merchant.data.mapper.MerchantSummaryMapper
import com.veyline.app.feature.merchant.data.remote.MerchantApiService
import com.veyline.app.feature.merchant.data.remote.model.MerchantSummaryDto
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MerchantPagingSourceTest {

    private val merchantSummaryMapper = MerchantSummaryMapper(
        imageUrlResolver = ImageUrlResolver(TEST_IMAGE_BASE_URL),
    )

    /** 验证首次加载成功时返回映射后的商家数据和下一页页码。 */
    @Test
    fun `load first page returns merchants and next page key`() = runTest {
        val cityCode = "city-a"
        val merchantDto = MerchantSummaryDto(
            id = "merchant-a",
            name = "商家甲",
            cityCode = cityCode,
            intro = "商家简介",
            coverPicture = "merchant-a.jpg",
        )

        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchants(
                page = 1,
                perPage = 12,
                cityCode = cityCode,
            )
        } returns Response.success(
            ApiResponse(
                code = ApiResponse.CODE_SUCCESS,
                msg = "success",
                data = PagedDataDto(
                    records = listOf(merchantDto),
                    total = 2,
                    size = 12,
                    current = 1,
                    pages = 2,
                ),
                fieldErrors = null,
            ),
        )

        val pagingSource = createPagingSource(
            apiService = apiService,
            cityCode = cityCode,
        )
        val loadParams = PagingSource.LoadParams.Refresh<Int>(
            key = null,
            loadSize = 12,
            placeholdersEnabled = false,
        )
        val loadResult = pagingSource.load(loadParams)

        assertIs<PagingSource.LoadResult.Page<Int, MerchantSummary>>(loadResult)

        val expectedMerchantSummary = MerchantSummary(
            id = "merchant-a",
            name = "商家甲",
            cityCode = cityCode,
            intro = "商家简介",
            coverImageUrl = "https://example.test/images/merchant-a.jpg",
        )
        assertEquals(listOf(expectedMerchantSummary), loadResult.data)
        assertEquals(null, loadResult.prevKey)
        assertEquals(2, loadResult.nextKey)
    }

    /** 验证加载最后一页成功时不再提供下一页页码。 */
    @Test
    fun `load last page returns null next page key`() = runTest {
        val cityCode = "city-a"
        val merchantDto = MerchantSummaryDto(
            id = "merchant-b",
            name = "商家乙",
            cityCode = cityCode,
            intro = "商家简介",
            coverPicture = null,
        )

        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchants(
                page = 2,
                perPage = 12,
                cityCode = cityCode,
            )
        } returns Response.success(
            ApiResponse(
                code = ApiResponse.CODE_SUCCESS,
                msg = "success",
                data = PagedDataDto(
                    records = listOf(merchantDto),
                    total = 2,
                    size = 12,
                    current = 2,
                    pages = 2,
                ),
            ),
        )

        val pagingSource = createPagingSource(
            apiService = apiService,
            cityCode = cityCode,
        )
        val loadParams = PagingSource.LoadParams.Append(
            key = 2,
            loadSize = 12,
            placeholdersEnabled = false,
        )
        val loadResult = pagingSource.load(loadParams)

        assertIs<PagingSource.LoadResult.Page<Int, MerchantSummary>>(loadResult)
        assertEquals(1, loadResult.data.size)
        assertEquals(null, loadResult.prevKey)
        assertEquals(null, loadResult.nextKey)
    }

    /** 验证第一页没有任何记录时返回正常空页面，而不是数据异常。 */
    @Test
    fun `load empty first page returns empty page`() = runTest {
        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchants(
                page = 1,
                perPage = 12,
                cityCode = null,
            )
        } returns Response.success(
            ApiResponse(
                code = ApiResponse.CODE_SUCCESS,
                msg = "success",
                data = PagedDataDto(
                    records = emptyList(),
                    total = 0,
                    size = 12,
                    current = 1,
                    pages = 0,
                ),
            ),
        )

        val pagingSource = createPagingSource(
            apiService = apiService,
            cityCode = null,
        )
        val loadParams = PagingSource.LoadParams.Refresh<Int>(
            key = null,
            loadSize = 12,
            placeholdersEnabled = false,
        )
        val loadResult = pagingSource.load(loadParams)

        assertIs<PagingSource.LoadResult.Page<Int, MerchantSummary>>(loadResult)
        assertEquals(emptyList(), loadResult.data)
        assertEquals(null, loadResult.prevKey)
        assertEquals(null, loadResult.nextKey)
    }

    /** 验证分页响应缺少 records 时返回数据无效错误。 */
    @Test
    fun `load response without records returns invalid data error`() = runTest {
        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchants(
                page = 1,
                perPage = 12,
                cityCode = null,
            )
        } returns Response.success(
            ApiResponse(
                code = ApiResponse.CODE_SUCCESS,
                msg = "success",
                data = PagedDataDto(
                    records = null, // 空页必须返回空数组，null 属于分页协议异常
                    total = 0,
                    size = 12,
                    current = 1,
                    pages = 1,
                ),
            ),
        )

        val pagingSource = createPagingSource(
            apiService = apiService,
            cityCode = null,
        )
        val loadParams = PagingSource.LoadParams.Refresh<Int>(
            key = null,
            loadSize = 12,
            placeholdersEnabled = false,
        )
        val loadResult = pagingSource.load(loadParams)

        assertIs<PagingSource.LoadResult.Error<Int, MerchantSummary>>(loadResult)
        assertIs<InvalidApiDataException>(loadResult.throwable)
    }

    /** 验证服务端返回页码与请求页码不一致时返回数据无效错误。 */
    @Test
    fun `load response with mismatched current page returns invalid data error`() = runTest {
        val apiService = mockk<MerchantApiService>()
        coEvery {
            apiService.getMerchants(
                page = 2,
                perPage = 12,
                cityCode = null,
            )
        } returns Response.success(
            ApiResponse(
                code = ApiResponse.CODE_SUCCESS,
                msg = "success",
                data = PagedDataDto(
                    records = emptyList(),
                    total = 0,
                    size = 12,
                    current = 1, // 请求第 2 页，但响应声明当前为第 1 页
                    pages = 2,
                ),
            ),
        )

        val pagingSource = createPagingSource(
            apiService = apiService,
            cityCode = null,
        )
        val loadParams = PagingSource.LoadParams.Append(
            key = 2,
            loadSize = 12,
            placeholdersEnabled = false,
        )
        val loadResult = pagingSource.load(loadParams)

        assertIs<PagingSource.LoadResult.Error<Int, MerchantSummary>>(loadResult)
        assertIs<InvalidApiDataException>(loadResult.throwable)
    }

    /** 验证业务失败被包装为保留原始失败分类的 Paging 加载错误。 */
    @Ignore("待补充 PagingFailureException 业务失败测试")
    @Test
    fun `load with business failure returns paging failure error`() {
    }

    /** 验证网络失败被包装为 Paging 加载错误，并保留原始异常原因。 */
    @Ignore("待补充 PagingFailureException 网络失败测试")
    @Test
    fun `load with network failure returns paging failure error with cause`() {
    }

    /** 验证同一页包含重复商家 ID 时只保留第一次出现的数据。 */
    @Ignore("待补充页内重复 ID 测试")
    @Test
    fun `load page with duplicate merchant ids keeps first occurrence`() {
    }

    /** 验证后续页包含已加载商家 ID 时只保留第一次出现的数据。 */
    @Ignore("待补充跨页重复 ID 测试")
    @Test
    fun `load next page with duplicate merchant ids keeps first occurrence`() {
    }

    private fun createPagingSource(
        apiService: MerchantApiService,
        cityCode: String? = null,
    ): MerchantPagingSource =
        MerchantPagingSource(
            apiService = apiService,
            merchantSummaryMapper = merchantSummaryMapper,
            cityCode = cityCode,
        )

    private companion object {
        const val TEST_IMAGE_BASE_URL = "https://example.test/images/"
    }
}
