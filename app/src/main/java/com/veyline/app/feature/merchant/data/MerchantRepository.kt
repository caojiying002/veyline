package com.veyline.app.feature.merchant.data

import com.veyline.app.data.network.apiCall
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.data.network.model.ApiResult
import com.veyline.app.feature.merchant.data.mapper.toDomainModels
import com.veyline.app.feature.merchant.data.remote.MerchantApiService
import com.veyline.app.feature.merchant.data.remote.model.MerchantCityDto
import com.veyline.app.feature.merchant.domain.model.MerchantCity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantRepository @Inject constructor(
    private val apiService: MerchantApiService,
) {

    suspend fun getMerchantCities(): ApiResult<List<MerchantCity>> {
        val result = apiCall { apiService.getMerchantCities() }

        return when (result) {
            is ApiResult.Success -> mapCities(result.data)
            is ApiResult.Failure -> result
        }
    }

    private fun mapCities(
        cities: List<MerchantCityDto>,
    ): ApiResult<List<MerchantCity>> =
        try {
            ApiResult.Success(cities.toDomainModels())
        } catch (exception: InvalidApiDataException) {
            ApiResult.Failure.Serialization(exception)
        }
}
