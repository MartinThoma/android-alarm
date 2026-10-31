import com.android.build.api.dsl.LibraryExtension
import com.martin_thoma.drift.buildlogic.DriftSdk
import com.martin_thoma.drift.buildlogic.JAVA_VERSION
import com.martin_thoma.drift.buildlogic.configureDriftLint
import com.martin_thoma.drift.buildlogic.configureDriftUnitTests
import com.martin_thoma.drift.buildlogic.configureKotlin
import com.martin_thoma.drift.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                compileSdk = DriftSdk.COMPILE
                defaultConfig {
                    minSdk = DriftSdk.MIN
                }
                compileOptions {
                    sourceCompatibility = JAVA_VERSION
                    targetCompatibility = JAVA_VERSION
                }
                lint {
                    targetSdk = DriftSdk.TARGET
                    configureDriftLint()
                }
                testOptions { configureDriftUnitTests() }
            }
            configureKotlin()

            dependencies {
                add("testImplementation", libs.findLibrary("junit4").get())
            }
        }
    }
}
