plugins {
    alias(libs.plugins.tide.android.feature)
}

android {
    namespace = "com.martin_thoma.tide.feature.alarm"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
}
