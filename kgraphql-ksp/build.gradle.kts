plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // KSP2 API — compileOnly, provided by the KSP gradle plugin at processing time.
    // The processor references annotations by FQN string (no compile dependency
    // on :shared), keeping this module decoupled from the application code.
    compileOnly(libs.ksp.symbol.processing.api)
}
