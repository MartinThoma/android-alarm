plugins {
    alias(libs.plugins.drift.android.library)
    alias(libs.plugins.drift.hilt)
}

android {
    namespace = "com.martin_thoma.drift.core.ringing"

    // Robolectric tests need the module's resources.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(project(":core:data"))
    api(project(":core:domain"))
    implementation(project(":core:notifications"))
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}
