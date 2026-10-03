package com.veyline.app.feature.auth.data.remote.model

import com.squareup.moshi.JsonClass

/**
 * 登录接口的请求 Body
 *
 * @property name 用户名
 * @property password 密码
 */
@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    val name: String,
    val password: String,
)
