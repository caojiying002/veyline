package com.veyline.app.feature.profile.data.remote

import com.veyline.app.data.annotation.TokenPolicy
import com.veyline.app.data.annotation.TokenPolicy.Policy
import com.veyline.app.data.network.model.ApiResponseDto
import com.veyline.app.feature.profile.data.remote.model.UserProfileDto
import retrofit2.Response
import retrofit2.http.GET

/**
 * Profile feature 使用的 Retrofit API 接口。
 *
 * 所有接口均使用相对于 API Base URL 的路径。本接口只负责描述 HTTP 请求与响应类型，
 * 不进行 DTO 转换或错误模型映射。
 */
interface ProfileApiService {

    /**
     * 获取当前登录用户的个人资料。
     *
     * 服务端根据请求头携带的 Token 识别用户，无需传入用户 ID。
     *
     * @return 包含当前用户资料的通用 API 响应
     */
    @TokenPolicy(Policy.REQUIRED)
    @GET("authUser/detail.json")
    suspend fun getProfile(): Response<ApiResponseDto<UserProfileDto>>
}