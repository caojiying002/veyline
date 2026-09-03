package com.veyline.app.di.qualifier

import javax.inject.Qualifier

/** 标记只用于加载图片的 OkHttpClient。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ImageHttpClient
