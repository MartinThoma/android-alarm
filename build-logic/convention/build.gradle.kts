plugins {
    `kotlin-dsl`
}

group = "com.martin_thoma.tide.buildlogic"

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.room.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "tide.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "tide.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "tide.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "tide.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("jvmLibrary") {
            id = "tide.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("room") {
            id = "tide.room"
            implementationClass = "RoomConventionPlugin"
        }
        register("hilt") {
            id = "tide.hilt"
            implementationClass = "HiltConventionPlugin"
        }
    }
}
