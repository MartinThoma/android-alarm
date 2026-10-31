plugins {
    alias(libs.plugins.tide.android.library)
    alias(libs.plugins.tide.hilt)
}

android {
    namespace = "com.martin_thoma.tide.core.data"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:database"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
}
