plugins {
    alias(libs.plugins.tide.android.library)
    alias(libs.plugins.tide.android.compose)
}

android {
    namespace = "com.martin_thoma.tide.core.designsystem"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
}
