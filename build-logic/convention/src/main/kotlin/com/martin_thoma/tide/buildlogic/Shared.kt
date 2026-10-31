package com.martin_thoma.tide.buildlogic

import com.android.build.api.dsl.Lint
import com.android.build.api.dsl.TestOptions
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** SDK levels for every module. See docs/requirements.md, section 2. */
object TideSdk {
    const val MIN = 35
    const val TARGET = 37
    const val COMPILE = 37
}

val JAVA_VERSION = JavaVersion.VERSION_21

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureKotlin() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(JAVA_VERSION.toString()))
            allWarningsAsErrors.set(true)
        }
    }
}

/** Unit tests may use Robolectric. Modules that do also set `unitTests.isIncludeAndroidResources`. */
internal fun TestOptions.configureTideUnitTests() {
    unitTests.all {
        // Robolectric reads FileDescriptor internals, which Java 17+ hides by default.
        it.jvmArgs("--add-exports", "java.base/jdk.internal.access=ALL-UNNAMED")
    }
}

internal fun Lint.configureTideLint() {
    warningsAsErrors = true
    abortOnError = true
    // Dependency updates come from Dependabot; they must not break the build.
    disable +=
        setOf(
            "AndroidGradlePluginVersion",
            "GradleDependency",
            "NewerVersionAvailable",
            "OldTargetApi",
        )
}
