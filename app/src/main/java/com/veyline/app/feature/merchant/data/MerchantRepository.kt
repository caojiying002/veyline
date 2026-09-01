package com.veyline.app.feature.merchant.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.veyline.app.data.network.apiCall
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.data.network.model.ApiResult
import com.veyline.app.feature.merchant.data.mapper.MerchantCityMapper
import com.veyline.app.feature.merchant.data.mapper.MerchantSummaryMapper
import com.veyline.app.feature.merchant.data.paging.MerchantPagingSource
import com.veyline.app.feature.merchant.data.remote.MerchantApiService
import com.veyline.app.feature.merchant.data.remote.model.MerchantCityDto
import com.veyline.app.feature.merchant.domain.model.MerchantCity
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提供 Merchant feature 所需的数据，并隔离网络模型与领域模型之间的转换细节。
 *
 * 商家列表使用 Paging 3 按城市分页加载，不在 Repository 中缓存 [PagingData] 或保存
 * 已加载页面。分页流的生命周期缓存由 ViewModel 根据自身作用域通过 `cachedIn()` 管理。
 *
 * 商家城市列表在首次成功加载后缓存在当前应用进程中。Repository 由 Hilt 以单例形式
 * 提供，因此同一进程内的调用方共享缓存；请求或数据转换失败时不写入缓存，后续调用仍可
 * 重新请求。
 */
@Singleton
class MerchantRepository @Inject constructor(
    private val apiService: MerchantApiService,
    private val merchantSummaryMapper: MerchantSummaryMapper,
) {

    /** 同步缓存访问，并避免多个首次调用同时发起相同的城市列表请求。 */
    private val cityCacheMutex = Mutex()

    /** 已成功转换的进程内缓存；`null` 表示尚未成功加载，空列表表示已成功加载但没有数据。 */
    private var cachedCities: List<MerchantCity>? = null

    /**
     * 创建按城市筛选的商家列表分页流。
     *
     * 城市代码会先去除首尾空白，空内容统一转换为 `null`，使 Retrofit 省略对应查询参数。
     * 每次调用都会创建独立的 [Pager] 和 [MerchantPagingSource]；切换筛选条件时，新数据源
     * 具有独立的加载和去重状态。
     *
     * 首次加载数量必须与普通分页数量保持一致，否则页码型接口会因前后请求采用不同的每页
     * 数量而产生数据重叠或遗漏。返回的流没有在 Repository 中调用 `cachedIn()`，该操作应
     * 由 ViewModel 结合自身生命周期完成。
     *
     * @param cityCode 城市筛选代码；`null` 或空白内容表示不限制城市。
     * @return 按当前城市条件分页加载的商家列表数据流。
     */
    fun getMerchants(
        cityCode: String?,
    ): Flow<PagingData<MerchantSummary>> {
        val apiCityCode = cityCode
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        return Pager(
            config = PagingConfig(
                pageSize = MERCHANT_PAGE_SIZE,
                initialLoadSize = MERCHANT_PAGE_SIZE,
                prefetchDistance = MERCHANT_PREFETCH_DISTANCE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                MerchantPagingSource(
                    apiService = apiService,
                    merchantSummaryMapper = merchantSummaryMapper,
                    cityCode = apiCityCode,
                )
            },
        ).flow
    }

    /**
     * 获取可用于商家筛选的城市列表。
     *
     * 缓存命中时直接返回；缓存未命中时，在互斥区内完成网络请求、数据校验与缓存写入，
     * 从而让并发调用复用第一次成功加载的结果。
     */
    suspend fun getMerchantCities(): ApiResult<List<MerchantCity>> =
        cityCacheMutex.withLock {
            cachedCities?.let { cities ->
                return@withLock ApiResult.Success(cities)
            }

            when (val result = apiCall { apiService.getMerchantCities() }) {
                is ApiResult.Success -> {
                    val mappedResult = mapCities(result.data)

                    // 空城市列表不写入缓存，使页面后续加载时能够重新请求并自行恢复。
                    if (mappedResult is ApiResult.Success && mappedResult.data.isNotEmpty()) {
                        cachedCities = mappedResult.data
                    }

                    mappedResult
                }

                is ApiResult.Failure -> result
            }
        }

    /** 将网络模型转换为领域模型，并把已知的数据协议异常转换为稳定的失败结果。 */
    private fun mapCities(
        cities: List<MerchantCityDto>,
    ): ApiResult<List<MerchantCity>> =
        try {
            ApiResult.Success(MerchantCityMapper.map(cities))
        } catch (exception: InvalidApiDataException) {
            ApiResult.Failure.Serialization(exception)
        }

    private companion object {
        /** 商家列表每页请求数量，同时用作首次加载数量以保持页码区间稳定。 */
        const val MERCHANT_PAGE_SIZE = 12

        /** 距离列表末尾剩余该数量的条目时，提前请求下一页。 */
        const val MERCHANT_PREFETCH_DISTANCE = 3
    }
}
