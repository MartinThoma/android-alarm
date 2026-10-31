package com.martin_thoma.drift.buildlogic

import com.android.build.api.dsl.Lint
import com.android.build.api.dsl.TestOptions
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

/** SDK levels for every module. See docs/requirements.md, section 2. */
object DriftSdk {
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
internal fun TestOptions.configureDriftUnitTests() {
    unitTests.all {
        // Robolectric reads FileDescriptor internals, which Java 17+ hides by default.
        it.jvmArgs("--add-exports", "java.base/jdk.internal.access=ALL-UNNAMED")
        // The same zone as the tests' fake clock, so times read the same on every machine, CI included.
        it.systemProperty("user.timezone", "Europe/Berlin")
        // A failing test shows its assertion and stack trace right in the build log.
        it.testLogging {
            events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }
}

internal fun Lint.configureDriftLint() {
    warningsAsErrors = true
    abortOnError = true
    // Lint's Kotlin analysis crashes now and then on test sources (a bug in lint, not in our code);
    // the app's own code is still checked in full.
    ignoreTestSources = true
    // Each finding, with file and line, right in the build log.
    textReport = true
    textOutput = File("stdout")
    explainIssues = false
    // Dependency updates come from Dependabot; they must not break the build.
    disable +=
        setOf(
            "AndroidGradlePluginVersion",
            "GradleDependency",
            "NewerVersionAvailable",
            "OldTargetApi",
        )
}
