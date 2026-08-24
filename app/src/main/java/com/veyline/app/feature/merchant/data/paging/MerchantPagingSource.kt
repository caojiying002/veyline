package com.veyline.app.feature.merchant.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.veyline.app.data.network.apiCall
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.data.network.model.ApiResult
import com.veyline.app.data.network.model.PagedDataDto
import com.veyline.app.feature.merchant.data.mapper.toDomainModels
import com.veyline.app.feature.merchant.data.remote.MerchantApiService
import com.veyline.app.feature.merchant.data.remote.model.MerchantSummaryDto
import com.veyline.app.feature.merchant.domain.model.MerchantSummary

class MerchantPagingSource(
    private val apiService: MerchantApiService,
    private val cityCode: String?,
): PagingSource<Int, MerchantSummary>() {

    // TODO 后续在 PagingSource 实例范围内统一处理页内和跨页重复 ID

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MerchantSummary> {
        val requestedPage = params.key ?: FIRST_PAGE

        val result = apiCall {
            apiService.getMerchants(
                page = requestedPage,
                perPage = params.loadSize,
                cityCode = cityCode,
            )
        }

        return when (result) {
            is ApiResult.Success -> createLoadResult(
                requestedPage = requestedPage,
                pagedDataDto = result.data
            )
            is ApiResult.Failure -> TODO("将结构化失败适配为 LoadResult.Error")
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MerchantSummary>): Int? {
        return null
    }

    private fun createLoadResult(
        requestedPage: Int,
        pagedDataDto: PagedDataDto<MerchantSummaryDto>
    ): LoadResult<Int, MerchantSummary> {
        try {
            val records = pagedDataDto.records
            val current = pagedDataDto.current
            val pages = pagedDataDto.pages

            if (records == null || current == null || pages == null) {
                throw InvalidApiDataException(
                    "Merchant page response is missing required pagination data",
                )
            }

            // 严格校验服务端分页元数据，避免错误页码造成重复加载或分页循环
            if (current < FIRST_PAGE || pages < 0 || current != requestedPage) {
                throw InvalidApiDataException(
                    "Merchant page response contains invalid pagination metadata",
                )
            }

            val merchantSummaries = records.toDomainModels()
            return LoadResult.Page(
                data = merchantSummaries,
                prevKey = null,
                nextKey = if (current >= pages) null else current + 1
            )
        } catch (exception: InvalidApiDataException) {
            return LoadResult.Error(exception)
        }
    }

    private companion object {
        // TODO 后续抽取为项目页码分页协议的公共常量
        const val FIRST_PAGE = 1
    }
}
