import com.android.build.api.dsl.LibraryExtension
import com.martin_thoma.tide.buildlogic.JAVA_VERSION
import com.martin_thoma.tide.buildlogic.TideSdk
import com.martin_thoma.tide.buildlogic.configureKotlin
import com.martin_thoma.tide.buildlogic.configureTideLint
import com.martin_thoma.tide.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                compileSdk = TideSdk.COMPILE
                defaultConfig {
                    minSdk = TideSdk.MIN
                }
                compileOptions {
                    sourceCompatibility = JAVA_VERSION
                    targetCompatibility = JAVA_VERSION
                }
                lint {
                    targetSdk = TideSdk.TARGET
                    configureTideLint()
                }
            }
            configureKotlin()

            dependencies {
                add("testImplementation", libs.findLibrary("junit4").get())
            }
        }
    }
}
