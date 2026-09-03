package com.veyline.app

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    /**
     * 以 [dagger.Lazy] 持有 Hilt 提供的 Coil3 [ImageLoader]，把它（连同专用 OkHttpClient、
     * 磁盘缓存等）的构建推迟到首次加载图片时，避免占用冷启动的主线程时间。
     */
    @Inject
    lateinit var imageLoader: dagger.Lazy<ImageLoader>

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        SingletonImageLoader.setSafe {
            imageLoader.get()
        }
    }

    companion object {

        /**
         * 为无法方便地通过依赖注入获取 Context 的旧式代码或特殊调用入口提供 Application。
         *
         * 这是早期 Android 项目中常见的全局访问方式，只能在 [Application.onCreate] 执行后
         * 使用，也不利于替换依赖和隔离测试。新代码应优先使用构造注入、`@ApplicationContext`
         * 或 Hilt EntryPoint；只有调用环境确实无法采用这些方式时才使用本 [INSTANCE]。
         */
        @JvmStatic
        lateinit var INSTANCE: App
            private set
    }
}
