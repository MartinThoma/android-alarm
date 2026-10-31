plugins {
    alias(libs.plugins.tide.android.feature)
}

android {
    namespace = "com.martin_thoma.tide.feature.timer"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:notifications"))
    implementation(project(":core:ringing"))
    implementation(libs.androidx.core.ktx)
}
