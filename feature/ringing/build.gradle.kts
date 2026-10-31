plugins {
    alias(libs.plugins.tide.android.feature)
}

android {
    namespace = "com.martin_thoma.tide.feature.ringing"
}

dependencies {
    implementation(project(":core:ringing"))
    implementation(libs.androidx.activity.compose)
}
