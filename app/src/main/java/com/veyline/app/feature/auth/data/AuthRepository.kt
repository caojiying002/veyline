package com.veyline.app.feature.auth.data

import com.veyline.app.data.auth.AuthTokenStore
import com.veyline.app.data.network.apiCall
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.data.network.result.ApiResult
import com.veyline.app.feature.auth.data.remote.AuthApiService
import com.veyline.app.feature.auth.data.remote.model.LoginRequestDto
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提供 Auth feature 所需的数据，并负责保存和清除 Token：登录成功后保存，退出登录时清除。
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: AuthApiService,
    private val authTokenStore: AuthTokenStore,
) {

    /**
     * 使用用户名和密码登录。
     *
     * 登录成功后会去掉 Token 首尾的空白，再保存到 [AuthTokenStore]。调用方不需要关心 Token
     * 的内容和存储方式，返回的 [ApiResult.Success] 也不携带 Token。
     *
     * @param name 用户名
     * @param password 密码
     * @return 登录是否成功；网络错误、业务错误或 Token 保存失败，都会返回对应的失败结果
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

    /**
     * 退出登录。
     *
     * 先请求服务端注销当前会话，再清除 [AuthTokenStore] 中保存的 Token。顺序不能反，因为
     * 注销请求需要携带 Token。
     *
     * 服务端请求的结果不影响退出：网络失败、服务端返回错误，或者本地原本就没有 Token，
     * 都会照常清除 Token。协程被取消时，只会中断服务端请求，Token 仍然会被清除。
     *
     * @return Token 已清除时返回 [ApiResult.Success]；只有清除 Token 失败（用户仍处于登录
     * 状态）时才返回 [ApiResult.Failure]
     */
    suspend fun logout(): ApiResult<Unit> {
        try {
            try {
                // 有意忽略服务端请求的结果：无论成功与否，都要退出登录
                apiCall { apiService.logout() }
            } finally {
                // 只有本地清除 Token 不可取消，服务端请求仍然允许取消
                withContext(NonCancellable) {
                    authTokenStore.clearToken()
                }
            }
            return ApiResult.Success(Unit)
        } catch (exception: IOException) {
            // 协程取消与 DataStore 写入失败同时发生的概率极低；一旦发生，finally 中的
            // IOException 会顶替正在传播的取消，这里重新抛出取消，避免吞掉它
            currentCoroutineContext().ensureActive()
            return ApiResult.Failure.Unexpected(exception)
        }
    }
}