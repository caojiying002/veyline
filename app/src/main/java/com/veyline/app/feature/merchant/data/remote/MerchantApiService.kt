package com.veyline.app.feature.merchant.data.remote

import com.veyline.app.data.network.model.ApiResponseDto
import com.veyline.app.data.network.model.PagedDataDto
import com.veyline.app.feature.merchant.data.remote.model.MerchantCityDto
import com.veyline.app.feature.merchant.data.remote.model.MerchantSummaryDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Merchant feature 使用的 Retrofit API 接口。
 *
 * 所有接口均使用相对于 API Base URL 的路径。本接口只负责描述 HTTP 请求与响应类型，
 * 不进行分页校验、DTO 转换或错误模型映射。
 */
interface MerchantApiService {

    /**
     * 获取可用于筛选商家列表的城市。
     *
     * @return 包含商家城市网络模型列表的通用 API 响应。
     */
    @GET("config/merchantCity.json")
    suspend fun getMerchantCities(): Response<ApiResponseDto<List<MerchantCityDto>>>

    /**
     * 按城市获取商家摘要分页数据。
     *
     * [perPage] 不提供客户端默认值，调用方必须明确传入与 Paging 配置一致的每页数量，
     * 避免 Retrofit 默认值、Paging 配置和服务端默认值不一致。
     *
     * @param page 请求页码；接口约定从 `1` 开始。
     * @param perPage 本次请求的每页数量。
     * @param cityCode 城市代码；传入 `null` 时 Retrofit 省略该查询参数，表示查询全部城市。
     * @return 包含商家摘要分页数据的通用 API 响应。
     */
    @GET("merchant/page.json")
    suspend fun getMerchants(
        @Query("page") page: Int,
        @Query("perPage") perPage: Int,
        @Query("cityCode") cityCode: String?,
    ): Response<ApiResponseDto<PagedDataDto<MerchantSummaryDto>>>
}
