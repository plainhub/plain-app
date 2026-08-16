plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.ismartcoding.plain.lib"
        compileSdk = 37
        minSdk = 28

        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
            api(libs.ktor.client.core)

            // Compose Multiplatform — required by vendored markdown renderer
            api(libs.runtime)
            api(libs.ui)
            api(libs.foundation)
            api(libs.material3)
            api(libs.navigation.compose)

            // IntelliJ Markdown — the parser/AST used by the renderer
            api(libs.jetbrains.markdown)
            api(libs.kotlinx.collections.immutable)

            // LaTeX math rendering
            api(libs.latex.base)
            api(libs.latex.parser)
            api(libs.latex.renderer)

            // Coil 3 — image loading for markdown images
            api(libs.coil)
            api(libs.coil.compose)
        }

        androidMain.dependencies {
            api(libs.androidx.core.ktx)
            // kotlin-reflect provides kotlin.reflect.full.createType used by
            // the Android ReflectionBridge actual for createKType.
            implementation(libs.kotlin.reflect)
        }
    }
}
