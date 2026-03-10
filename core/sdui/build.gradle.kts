plugins {
    alias(libs.plugins.rickandmorty.android.library.compose)
    alias(libs.plugins.rickandmorty.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.rickandmorty.core.sdui"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:ui"))

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.ktx)

    // Immutable collections
    implementation(libs.kotlinx.collections.immutable)
}
