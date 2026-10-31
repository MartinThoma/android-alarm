plugins {
    alias(libs.plugins.tide.android.library)
    alias(libs.plugins.tide.hilt)
}

android {
    namespace = "com.martin_thoma.tide.core.scheduling"
}

dependencies {
    api(project(":core:data"))
    api(project(":core:domain"))
}
