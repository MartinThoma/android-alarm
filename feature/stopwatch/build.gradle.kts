plugins {
    alias(libs.plugins.drift.android.feature)
}

android {
    namespace = "com.martin_thoma.drift.feature.stopwatch"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:notifications"))
}
