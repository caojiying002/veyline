package com.veyline.app.feature.auth.data

import com.veyline.app.data.auth.AuthTokenStore
import com.veyline.app.data.network.apiCall
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.data.network.result.ApiResult
import com.veyline.app.feature.auth.data.remote.AuthApiService
import com.veyline.app.feature.auth.data.remote.model.LoginRequestDto
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提供 Auth feature 所需的数据，并在登录成功后负责保存登录凭证。
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: AuthApiService,
    private val authTokenStore: AuthTokenStore,
) {

    /**
     * 使用用户名和密码登录
     *
     * 登录成功后会把 Token 清理并保存到 [AuthTokenStore]；调用方不需要关心 Token 的具体
     * 内容和持久化方式，返回的 [ApiResult.Success] 也不携带 Token。
     *
     * @param name 用户名
     * @param password 密码
     * @return 登录是否成功；网络、业务或 Token 保存失败均返回对应的失败结果
     */
    suspend fun login(name: String, password: String): ApiResult<Unit> {
        val result = apiCall {
            apiService.login(LoginRequestDto(name, password))
        }

        when (result) {
            is ApiResult.Failure -> return result

            is ApiResult.Success -> {
                val token = result.data.trim().ifEmpty {
                    return ApiResult.Failure.Serialization(
                        InvalidApiDataException("Login response contains an empty authentication token"),
                    )
                }

                try {
                    authTokenStore.saveToken(token)
                    return ApiResult.Success(Unit)
                } catch (exception: IOException) {
                    // DataStore 写入失败极少发生，且目前只有这一个调用点，不单独为此新增失败类型
                    return ApiResult.Failure.Unexpected(exception)
                }
            }
        }
    }
}