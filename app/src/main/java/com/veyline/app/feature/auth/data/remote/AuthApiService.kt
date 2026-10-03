package com.veyline.app.feature.auth.data.remote

import com.veyline.app.data.network.model.ApiResponseDto
import com.veyline.app.feature.auth.data.remote.model.LoginRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Auth feature 使用的 Retrofit API 接口。
 *
 * 所有接口均使用相对于 API Base URL 的路径。本接口只负责描述 HTTP 请求与响应类型，
 * 不处理登录态持久化，也不负责生成用户提示文案。
 */
interface AuthApiService {

    /**
     * 使用用户名和密码登录
     *
     * 登录成功时 `data` 字段为一个 Token 字符串；用户名或密码格式错误时返回表单字段
     * 验证失败，由 [ApiResponseDto.fieldErrors] 承载。
     *
     * @param body 登录请求参数，包含用户名和密码
     * @return 包含登录 Token 的通用 API 响应
     */
    @POST("auth/login.json")
    suspend fun login(
        @Body body: LoginRequestDto,
    ): Response<ApiResponseDto<String>>
}
