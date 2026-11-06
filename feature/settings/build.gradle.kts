plugins {
    alias(libs.plugins.drift.android.feature)
}

android {
    namespace = "com.martin_thoma.drift.feature.settings"
}

dependencies {
    implementation(project(":core:notifications"))
}
