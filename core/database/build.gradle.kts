plugins {
    alias(libs.plugins.tide.android.library)
    alias(libs.plugins.tide.room)
    alias(libs.plugins.tide.hilt)
}

android {
    namespace = "com.martin_thoma.tide.core.database"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
