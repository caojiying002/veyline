package com.veyline.app.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.veyline.app.di.qualifier.AuthStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token 的持久化存储
 *
 * 只负责保存、读取、清除 Token，不涉及网络请求，也不判断具体的业务权限；是否登录这件事由
 * [isLoggedIn] 从 Token 是否为空派生得到，不单独维护一份登录状态。
 *
 * @param dataStore 保存 Token 的 Preferences DataStore
 */
@Singleton
class AuthTokenStore @Inject constructor(
    @AuthStore private val dataStore: DataStore<Preferences>,
) {

    /**
     * 当前 Token，未登录时为 `null`
     *
     * 读取失败时按官方推荐的做法降级为空的 Preferences，不让异常穿透给调用方。
     */
    val token: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
                return@catch
            }
            throw exception
        }
        .map { preferences ->
            preferences[TOKEN_KEY]
        }
        .distinctUntilChanged()

    /**
     * 当前是否已登录
     *
     * 这里单独做了一次 [distinctUntilChanged]：[token] 的去重只能过滤值完全相同的情况，
     * Token 刷新时新旧值都非空但内容不同，仍会连续发出两次 `true`。再去重一次可以保证只有
     * 登录状态真正发生变化时才会有新的发射，避免误判成一次新的登录。
     */
    val isLoggedIn: Flow<Boolean> = token
        .map { !it.isNullOrBlank() }
        .distinctUntilChanged()

    /** 一次性读取当前 Token，供无法直接订阅 Flow 的调用方使用（例如 OkHttp 拦截器） */
    suspend fun getToken(): String? = token.first()

    /** 保存 Token */
    suspend fun saveToken(token: String) {
        require(token.isNotBlank()) { "Authentication token must not be blank." }
        dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    /** 清除 Token，用于退出登录 */
    suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
        }
    }

    private companion object {
        val TOKEN_KEY = stringPreferencesKey("auth_token")
    }
}
