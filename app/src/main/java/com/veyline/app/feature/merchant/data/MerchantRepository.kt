package com.veyline.app.feature.merchant.data

import com.veyline.app.data.network.apiCall
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.data.network.model.ApiResult
import com.veyline.app.feature.merchant.data.mapper.toDomainModels
import com.veyline.app.feature.merchant.data.remote.MerchantApiService
import com.veyline.app.feature.merchant.data.remote.model.MerchantCityDto
import com.veyline.app.feature.merchant.domain.model.MerchantCity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提供 Merchant feature 所需的数据，并隔离网络模型与领域模型之间的转换细节。
 *
 * 商家城市列表在首次成功加载后缓存在当前应用进程中。Repository 由 Hilt 以单例形式
 * 提供，因此同一进程内的调用方共享缓存；请求或数据转换失败时不写入缓存，后续调用仍可
 * 重新请求。
 */
@Singleton
class MerchantRepository @Inject constructor(
    private val apiService: MerchantApiService,
) {

    /** 同步缓存访问，并避免多个首次调用同时发起相同的城市列表请求。 */
    private val cityCacheMutex = Mutex()

    /** 已成功转换的进程内缓存；`null` 表示尚未成功加载，空列表表示已成功加载但没有数据。 */
    private var cachedCities: List<MerchantCity>? = null

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

                    if (mappedResult is ApiResult.Success) {
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
            ApiResult.Success(cities.toDomainModels())
        } catch (exception: InvalidApiDataException) {
            ApiResult.Failure.Serialization(exception)
        }
}
