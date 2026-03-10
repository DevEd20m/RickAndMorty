plugins {
    alias(libs.plugins.rickandmorty.android.library)
}

android {
    namespace = "com.example.rickandmorty.core.presentation"
}

dependencies {
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
}
