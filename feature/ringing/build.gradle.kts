plugins {
    alias(libs.plugins.drift.android.feature)
}

android {
    namespace = "com.martin_thoma.drift.feature.ringing"
}

dependencies {
    implementation(project(":core:ringing"))
    implementation(libs.androidx.activity.compose)
}
