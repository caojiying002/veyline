plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
}

// 真实 API 地址由用户级 Gradle Property 或命令行 -P 参数提供，避免写入公开仓库。
// 未配置时使用保留的无效域名，使公开项目仍可正常编译和运行。
val apiBaseUrl = providers.gradleProperty("VEYLINE_API_BASE_URL")
    .orElse("https://example.invalid/")
    .get()
require(apiBaseUrl.startsWith("https://")) {
    "VEYLINE_API_BASE_URL must use HTTPS"
}
require(apiBaseUrl.endsWith('/')) {
    "VEYLINE_API_BASE_URL must end with '/'"
}

// 真实 API User-Agent 可由用户级 Gradle Property 或命令行 -P 参数覆盖。
// 未配置时使用公开的应用标识，不包含私有服务端的兼容性信息。
val apiUserAgent = providers.gradleProperty("VEYLINE_API_USER_AGENT")
    .orElse("Veyline/1.0 (Android)")
    .get()
require(apiUserAgent.isNotBlank()) {
    "VEYLINE_API_USER_AGENT must not be blank"
}
require('\r' !in apiUserAgent && '\n' !in apiUserAgent) {
    "VEYLINE_API_USER_AGENT must not contain line breaks"
}

android {
    namespace = "com.veyline.app"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.veyline.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 将构建时解析的地址写入 AGP 生成的 BuildConfig，供网络层统一读取。
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"$apiBaseUrl\"",
        )
        buildConfigField(
            type = "String",
            name = "API_USER_AGENT",
            value = "\"$apiUserAgent\"",
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            // 正式打release包不应该用debug版本的keystore，这里签名只是为了让release包能正常安装运行
            // 有时候需要查看release包运行起来的一些特性，比如日志打印是否隐藏
            //signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Kotlin 协程与序列化
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // View 体系基础组件（用于与 Compose 混合开发）
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)

    // 网络请求与 JSON 解析
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.converter.moshi)
    implementation(libs.squareup.moshi)
    ksp(libs.squareup.moshi.kotlin.codegen)
    implementation(libs.squareup.okhttp)
    implementation(libs.squareup.okhttp.logginginterceptor)

    // 依赖注入
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)
    implementation(libs.androidx.hilt)

    // Paging 3 分页加载
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // 测试依赖
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk.android)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.androidx.paging.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
