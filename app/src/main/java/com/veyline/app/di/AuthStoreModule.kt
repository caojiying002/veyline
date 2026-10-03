package com.veyline.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.veyline.app.di.qualifier.AuthStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 提供保存 Token 的 Preferences DataStore
 *
 * 单独用一份名为 `auth` 的存储文件，不跟其他业务数据混在一起，方便以后单独清理或替换存储方式。
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthStoreModule {

    /** 该文件 DataStore 的唯一创建点，进程内只有一个实例 */
    private val Context.authDataStore by preferencesDataStore(
        name = "auth"
    )

    @Provides
    @Singleton
    @AuthStore
    fun provideAuthStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.authDataStore
}
