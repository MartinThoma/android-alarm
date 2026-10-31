import com.martin_thoma.drift.buildlogic.JAVA_VERSION
import com.martin_thoma.drift.buildlogic.configureKotlin
import com.martin_thoma.drift.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** A pure Kotlin module without Android, e.g. `:core:model` and `:core:domain`. Tests run on the JVM. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
            configureKotlin()

            dependencies {
                add("testImplementation", libs.findLibrary("junit4").get())
            }
        }
    }
}
