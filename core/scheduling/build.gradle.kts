plugins {
    alias(libs.plugins.drift.android.library)
    alias(libs.plugins.drift.hilt)
}

android {
    namespace = "com.martin_thoma.drift.core.scheduling"
}

dependencies {
    api(project(":core:data"))
    api(project(":core:domain"))
    implementation(project(":core:ringing"))
}
