plugins {
    alias(libs.plugins.drift.android.library)
    alias(libs.plugins.drift.android.compose)
}

android {
    namespace = "com.martin_thoma.drift.core.designsystem"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
}
