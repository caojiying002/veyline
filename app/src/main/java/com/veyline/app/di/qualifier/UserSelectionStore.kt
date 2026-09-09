package com.veyline.app.di.qualifier

import javax.inject.Qualifier

/**
 * 标记保存用户各项选择（首页当前城市、商家当前省份等）的 Preferences DataStore。
 *
 * 用于与认证、设置等其他 DataStore 实例区分，避免注入 `DataStore<Preferences>` 时混用。
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserSelectionStore
