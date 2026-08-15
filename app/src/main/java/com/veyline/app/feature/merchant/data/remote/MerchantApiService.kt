package com.veyline.app.feature.merchant.data.remote

import com.veyline.app.data.network.model.ApiResponse
import com.veyline.app.feature.merchant.data.remote.model.MerchantCityDto
import retrofit2.Response
import retrofit2.http.GET

/** 商家 feature 的 Retrofit 接口定义。 */
interface MerchantApiService {

    /** 获取可用于筛选商家的城市列表。 */
    @GET("api/mobile/config/merchantCity.json")
    suspend fun getMerchantCities(): Response<ApiResponse<List<MerchantCityDto>>>
}
