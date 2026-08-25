package com.veyline.app.feature.merchant.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.veyline.app.data.network.apiCall
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.data.network.model.ApiResult
import com.veyline.app.data.network.model.PagedDataDto
import com.veyline.app.data.paging.FIRST_PAGE
import com.veyline.app.data.paging.PagingFailureException
import com.veyline.app.feature.merchant.data.mapper.toDomainModels
import com.veyline.app.feature.merchant.data.remote.MerchantApiService
import com.veyline.app.feature.merchant.data.remote.model.MerchantSummaryDto
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import java.util.logging.Logger

/**
 * 按城市筛选条件分页加载商家摘要数据。
 *
 * 本数据源使用从 [FIRST_PAGE] 开始的整数页码作为分页键，通过 [apiCall] 复用项目统一的
 * 网络错误分类，并在成功响应进入 Paging 3 前完成分页元数据校验和 DTO 映射。服务端返回
 * 的当前页必须与请求页一致，避免错误页码造成重复加载或分页循环。
 *
 * 去重状态只在当前 [MerchantPagingSource] 实例内生效，同时覆盖页内和跨页重复 ID。刷新
 * 或切换城市时，Paging 3 创建新的数据源实例，去重状态随之重置。为了保证页码对应的数据
 * 区间稳定，创建 Pager 时必须令 `initialLoadSize` 与 `pageSize` 保持一致。
 *
 * @property apiService 商家 API 服务。
 * @property cityCode 城市筛选代码；`null` 表示不限制城市。
 */
class MerchantPagingSource(
    private val apiService: MerchantApiService,
    private val cityCode: String?,
) : PagingSource<Int, MerchantSummary>() {

    /** 当前数据源实例已经输出的商家 ID，用于统一过滤页内和跨页重复数据。 */
    private val loadedMerchantIds = mutableSetOf<String>()

    /**
     * 加载指定页，并将网络调用结果转换为 Paging 3 的页面或错误结果。
     *
     * @param params 当前加载参数；`key` 为空时加载第一页，`loadSize` 作为接口的每页数量。
     */
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
                pagedDataDto = result.data,
            )
            is ApiResult.Failure -> LoadResult.Error(
                throwable = PagingFailureException(result),
            )
        }
    }

    /**
     * 返回 `null`，使主动刷新和筛选条件变化后的新数据源从第一页重新加载。
     *
     * 当前页面不保留围绕原可见位置加载的刷新键。
     */
    override fun getRefreshKey(state: PagingState<Int, MerchantSummary>): Int? {
        return null
    }

    /**
     * 校验成功响应的分页结构，将当前页记录映射并去重后生成 Paging 页面。
     *
     * 接口数据缺失、分页元数据不合法或当前页没有任何有效商家时返回
     * [LoadResult.Error]，避免将异常响应误判为空页或分页结束。
     */
    private fun createLoadResult(
        requestedPage: Int,
        pagedDataDto: PagedDataDto<MerchantSummaryDto>,
    ): LoadResult<Int, MerchantSummary> {
        try {
            // total 和 size 不参与下一页计算，当前分页逻辑只要求以下三个字段有效
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

            val merchants = records.toDomainModels()
            val deduplicatedMerchants = merchants.filter { merchant ->
                loadedMerchantIds.add(merchant.id)
            }

            // 发现重复 ID 时只记录数量，不输出具体 ID 或其他业务内容
            if (merchants.size != deduplicatedMerchants.size) {
                logger.warning(
                    "Merchant page response contains " +
                        "${merchants.size - deduplicatedMerchants.size} duplicate records; " +
                        "keeping first occurrences",
                )
            }

            return LoadResult.Page(
                data = deduplicatedMerchants,
                prevKey = null,
                nextKey = if (current >= pages) null else current + 1,
            )
        } catch (exception: InvalidApiDataException) {
            return LoadResult.Error(exception)
        }
    }

    private companion object {
        private val logger = Logger.getLogger("MerchantPagingSource")
    }
}
