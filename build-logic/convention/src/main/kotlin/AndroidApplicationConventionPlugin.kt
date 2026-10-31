import com.android.build.api.dsl.ApplicationExtension
import com.martin_thoma.drift.buildlogic.DriftSdk
import com.martin_thoma.drift.buildlogic.JAVA_VERSION
import com.martin_thoma.drift.buildlogic.configureDriftLint
import com.martin_thoma.drift.buildlogic.configureDriftUnitTests
import com.martin_thoma.drift.buildlogic.configureKotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                compileSdk = DriftSdk.COMPILE
                defaultConfig {
                    minSdk = DriftSdk.MIN
                    targetSdk = DriftSdk.TARGET
                }
                compileOptions {
                    sourceCompatibility = JAVA_VERSION
                    targetCompatibility = JAVA_VERSION
                }
                lint { configureDriftLint() }
                testOptions { configureDriftUnitTests() }
            }
            configureKotlin()
        }
    }
}
