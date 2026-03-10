plugins {
    alias(libs.plugins.rickandmorty.android.library)
    alias(libs.plugins.rickandmorty.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.rickandmorty.core.network"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Retrofit + OkHttp
    api(libs.retrofit)
    api(libs.retrofit.kotlinx.serialization.converter)
    api(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // kotlinx.serialization
    api(libs.kotlinx.serialization.json)

    // Coroutines
    api(libs.kotlinx.coroutines.android)
}
