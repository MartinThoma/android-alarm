plugins {
    alias(libs.plugins.drift.android.library)
    alias(libs.plugins.drift.hilt)
}

android {
    namespace = "com.martin_thoma.drift.core.data"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:database"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
}
