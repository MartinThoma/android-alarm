plugins {
    alias(libs.plugins.tide.android.application)
    alias(libs.plugins.tide.android.compose)
    alias(libs.plugins.tide.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.martin_thoma.tide"

    defaultConfig {
        applicationId = "com.martin_thoma.tide"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    androidResources {
        // Per-app language setting (requirements NF-07); locales come from res/values-*.
        generateLocaleConfig = true
    }

    // F-Droid rejects the encrypted dependency metadata block in the APK (requirements NF-05).
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:scheduling"))
    implementation(project(":feature:alarm"))
    implementation(project(":feature:stopwatch"))
    implementation(project(":feature:timer"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.core)
}
