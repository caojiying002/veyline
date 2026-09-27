package com.veyline.app.di.qualifier

import javax.inject.Qualifier

/**
 * 标记保存 Token 的 Preferences DataStore
 *
 * 项目里可能同时存在多个 DataStore 实例，用这个限定符区分注入目标，避免跟其他实例混淆。
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthStore
