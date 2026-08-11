plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
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
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // 测试依赖
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
