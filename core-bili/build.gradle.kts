plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        allWarningsAsErrors.set(false)
    }
}

dependencies {
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.junit)
}
