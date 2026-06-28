plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.devtools.ksp)
    id("androidx.room")
}

kotlin {
    jvmToolchain(17)

    androidLibrary {
        namespace = "com.ismartcoding.plain.shared"
        compileSdk = 37
        minSdk = 28

        // Required so that Compose Multiplatform resources (drawable/strings/etc.)
        // are packaged into the consuming Android app's assets. Without this the
        // new `com.android.kotlin.multiplatform.library` plugin disables Android
        // resource processing and `copyAndroidMainComposeResourcesToAndroidAssets`
        // is never wired up.
        androidResources {
            enable = true
        }

        // Enable Android host-side unit tests so `commonTest` (and any
        // androidUnitTest-specific sources) get compiled and runnable via
        // `testDebugUnitTest` / `testReleaseUnitTest`. Without this the
        // plugin silently ignores both source sets and `:shared:commonTest`
        // emits a warning on every build.
        withHostTest {}
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "PlainShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            api(compose.components.resources)
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.0")
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.compose.lifecycle.runtime)
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.kotlinx.serialization.json)
            api(libs.room.runtime)
            api(libs.ktor.client.core)
            implementation(libs.coil.compose)

            // Vendored multiplatform-markdown-renderer (lib/markdown/) depends on these.
            api(libs.jetbrains.markdown)
            api(libs.kotlinx.collections.immutable)
            api(libs.kotlinx.coroutines.core)

            // LaTeX math rendering (KMP — Compose Multiplatform renderer)
            api(libs.latex.base)
            api(libs.latex.parser)
            api(libs.latex.renderer)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.logging)
            implementation(libs.okhttp)
            implementation(libs.tink.android)
            implementation(libs.androidx.exifinterface)
            implementation(libs.coil)
            implementation(libs.coil.svg)
            implementation(libs.coil.gif)
            implementation(libs.coil.video)
            implementation(libs.coil.network.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.sqlite.bundled)
            implementation(libs.ktor.client.darwin)
            implementation(libs.coil)
            implementation(libs.coil.network.ktor3)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

compose.resources {
    packageOfResClass = "com.ismartcoding.plain.i18n"
    generateResClass = always
    publicResClass = true
}
