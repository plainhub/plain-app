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
        compileSdk = 36
        minSdk = 28

        // Required so that Compose Multiplatform resources (drawable/strings/etc.)
        // are packaged into the consuming Android app's assets. Without this the
        // new `com.android.kotlin.multiplatform.library` plugin disables Android
        // resource processing and `copyAndroidMainComposeResourcesToAndroidAssets`
        // is never wired up.
        androidResources {
            enable = true
        }
    }

    listOf(
        iosX64(),
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
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.kotlinx.serialization.json)
            api(libs.room.runtime)
        }
        iosMain.dependencies {
            implementation(libs.sqlite.bundled)
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

compose.resources {
    packageOfResClass = "com.ismartcoding.plain.i18n"
    generateResClass = always
    publicResClass = true
}
