plugins {
    alias(libs.plugins.tide.jvm.library)
}

dependencies {
    api(project(":core:model"))
}
