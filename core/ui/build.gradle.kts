plugins {
    alias(libs.plugins.rickandmorty.android.library.compose)
}

android {
    namespace = "com.example.rickandmorty.core.ui"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.ui)
    api(libs.androidx.ui.graphics)
    api(libs.androidx.ui.tooling.preview)
    api(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
}
