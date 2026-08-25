package com.veyline.app.feature.merchant.data.paging

import androidx.paging.PagingSource
import com.veyline.app.data.network.model.ApiResponse
import com.veyline.app.data.network.model.PagedDataDto
import com.veyline.app.feature.merchant.data.remote.MerchantApiService
import com.veyline.app.feature.merchant.data.remote.model.MerchantSummaryDto
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MerchantPagingSourceTest {

    /** 验证首次加载成功时返回映射后的商家数据和下一页页码。 */
    @Test
    fun `load first page returns merchants and next page key`() = runTest {
        val cityCode = "city-a"
        val merchantDto = MerchantSummaryDto(
            id = "merchant-a",
            name = "商家甲",
            cityCode = cityCode,
            intro = "商家简介",
            coverPicture = "images/merchant-a.jpg",
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

        val pagingSource = MerchantPagingSource(
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
            coverImagePath = "images/merchant-a.jpg",
        )
        assertEquals(listOf(expectedMerchantSummary), loadResult.data)
        assertEquals(null, loadResult.prevKey)
        assertEquals(2, loadResult.nextKey)
    }
}
