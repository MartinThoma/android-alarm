import com.android.build.api.dsl.ApplicationExtension
import com.martin_thoma.tide.buildlogic.JAVA_VERSION
import com.martin_thoma.tide.buildlogic.TideSdk
import com.martin_thoma.tide.buildlogic.configureKotlin
import com.martin_thoma.tide.buildlogic.configureTideLint
import com.martin_thoma.tide.buildlogic.configureTideUnitTests
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                compileSdk = TideSdk.COMPILE
                defaultConfig {
                    minSdk = TideSdk.MIN
                    targetSdk = TideSdk.TARGET
                }
                compileOptions {
                    sourceCompatibility = JAVA_VERSION
                    targetCompatibility = JAVA_VERSION
                }
                lint { configureTideLint() }
                testOptions { configureTideUnitTests() }
            }
            configureKotlin()
        }
    }
}
