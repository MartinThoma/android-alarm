plugins {
    alias(libs.plugins.drift.android.feature)
}

android {
    namespace = "com.martin_thoma.drift.feature.alarm"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:ringing"))
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.kotlinx.coroutines.test)
}
