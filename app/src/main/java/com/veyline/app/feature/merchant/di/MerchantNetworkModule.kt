package com.veyline.app.feature.merchant.di

import com.veyline.app.feature.merchant.data.remote.MerchantApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MerchantNetworkModule {

    @Provides
    @Singleton
    fun provideMerchantApiService(retrofit: Retrofit): MerchantApiService =
        retrofit.create(MerchantApiService::class.java)
}
