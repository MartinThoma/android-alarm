plugins {
    alias(libs.plugins.tide.android.library)
    alias(libs.plugins.tide.hilt)
}

android {
    namespace = "com.martin_thoma.tide.core.ringing"

    // Robolectric tests need the module's resources.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(project(":core:data"))
    api(project(":core:domain"))
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}
