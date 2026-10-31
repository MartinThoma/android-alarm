plugins {
    alias(libs.plugins.drift.jvm.library)
}

dependencies {
    api(project(":core:model"))
}
