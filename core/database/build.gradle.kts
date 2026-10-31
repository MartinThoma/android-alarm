plugins {
    alias(libs.plugins.drift.android.library)
    alias(libs.plugins.drift.room)
    alias(libs.plugins.drift.hilt)
}

android {
    namespace = "com.martin_thoma.drift.core.database"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
