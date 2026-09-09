package com.veyline.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.veyline.app.di.qualifier.UserSelectionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 提供保存用户各项选择的 Preferences DataStore。
 *
 * 全应用共用一个名为 `user_selections` 的存储文件，按业务拆分 key（首页当前城市、商家
 * 当前省份等）。调用方通过 [UserSelectionStore] 限定符注入，不感知文件名与构造方式。
 */
@Module
@InstallIn(SingletonComponent::class)
object UserSelectionModule {

    /** 该文件 DataStore 的唯一创建点，进程内仅一个实例；外部经 [provideUserSelectionStore] 注入。 */
    private val Context.userSelectionDataStore by preferencesDataStore(
        name = "user_selections"
    )

    @Provides
    @Singleton
    @UserSelectionStore
    fun provideUserSelectionStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.userSelectionDataStore
}
