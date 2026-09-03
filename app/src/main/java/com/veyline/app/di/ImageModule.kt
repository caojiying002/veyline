package com.veyline.app.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.veyline.app.BuildConfig
import com.veyline.app.data.image.ImageUrlResolver
import com.veyline.app.data.image.interceptor.ImageRequestHeadersInterceptor
import com.veyline.app.di.qualifier.ImageHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * 提供图片地址解析与 Coil 图片加载所需的应用级依赖。
 *
 * 图片请求使用独立的 OkHttpClient，避免继承 API 客户端的认证、日志或其他业务配置。
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageModule {

    @Provides
    @Singleton
    fun provideImageUrlResolver(): ImageUrlResolver =
        ImageUrlResolver(BuildConfig.IMAGE_BASE_URL)

    @Provides
    @Singleton
    @ImageHttpClient
    fun provideImageOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                ImageRequestHeadersInterceptor(
                    userAgent = BuildConfig.IMAGE_USER_AGENT,
                    referer = BuildConfig.IMAGE_REFERER,
                ),
            )
            .build()

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        @ImageHttpClient imageHttpClient: OkHttpClient,
    ): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { imageHttpClient },
                    ),
                )
            }
            .build()
}
