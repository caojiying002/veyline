package com.veyline.app.di

import com.veyline.app.BuildConfig
import com.veyline.app.data.image.ImageUrlResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageModule {

    @Provides
    @Singleton
    fun provideImageUrlResolver(): ImageUrlResolver =
        ImageUrlResolver(BuildConfig.IMAGE_BASE_URL)
}