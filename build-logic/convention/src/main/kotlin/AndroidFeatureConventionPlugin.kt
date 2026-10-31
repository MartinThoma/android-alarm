import com.martin_thoma.tide.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * A `:feature:*` module: an Android library with Compose and Navigation 3 routes.
 * Feature modules depend on `:core:*` modules only, never on each other.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("tide.android.library")
            pluginManager.apply("tide.android.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                add("implementation", project(":core:designsystem"))
                add("implementation", libs.findLibrary("androidx-navigation3-runtime").get())
                add("implementation", libs.findLibrary("kotlinx-serialization-core").get())
            }
        }
    }
}
