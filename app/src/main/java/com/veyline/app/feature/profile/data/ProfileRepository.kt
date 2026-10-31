package com.veyline.app.feature.profile.data

import com.veyline.app.data.network.apiCall
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.data.network.result.ApiResult
import com.veyline.app.feature.profile.data.mapper.UserProfileMapper
import com.veyline.app.feature.profile.data.remote.ProfileApiService
import com.veyline.app.feature.profile.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提供 Profile feature 所需的数据，并隔离网络模型与领域模型之间的转换细节。
 *
 * 用户资料不在 Repository 中缓存，每次调用都会向服务端请求最新数据。
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val apiService: ProfileApiService,
) {

    /**
     * 获取当前登录用户的个人资料。
     *
     * 本方法要求已登录：没有 Token 时网络请求不会发出，返回
     * [ApiResult.Failure.AuthRequired]。
     *
     * @return 转换后的用户资料，或对应的未登录、网络请求、数据校验失败结果
     */
    suspend fun getProfile(): ApiResult<UserProfile> {
        val result = apiCall {
            apiService.getProfile()
        }

        return when (result) {
            is ApiResult.Success -> {
                try {
                    ApiResult.Success(
                        data = UserProfileMapper.map(result.data),
                    )
                } catch (exception: InvalidApiDataException) {
                    ApiResult.Failure.Serialization(exception)
                }
            }

            is ApiResult.Failure -> result
        }
    }
}
