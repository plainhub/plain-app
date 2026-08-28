import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    id("kotlin-parcelize")
    kotlin("plugin.serialization") version libs.versions.kotlin
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.play.publisher)
}

val keystoreProperties = Properties()
rootProject.file("keystore.properties").let {
    if (it.exists()) {
        keystoreProperties.load(FileInputStream(it))
    }
}

android {
    compileSdk = 37
    defaultConfig {
        applicationId = "com.ismartcoding.plain"
        minSdk = 28
        targetSdk = 36

        val abiFilterList = if (hasProperty("abiFilters")) property("abiFilters").toString().split(';') else listOf()
        val singleAbiNum =
            when (abiFilterList.takeIf { it.size == 1 }?.first()) {
                "armeabi-v7a" -> 2
                "arm64-v8a" -> 1
                else -> 0
            }

        val vCode = 661
        versionCode = vCode - singleAbiNum
        versionName = "3.3.10"

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += abiFilterList.ifEmpty {
                listOf("arm64-v8a")
            }
        }
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
            storeFile = file(keystoreProperties.getProperty("storeFile", "release.jks"))
            storePassword = keystoreProperties.getProperty("storePassword")
        }
        // Pin debug keystore to user-level ~/.android/debug.keystore so CI/agent builds
        // sign with the same key as Android Studio (avoids INSTALL_FAILED_UPDATE_INCOMPATIBLE).
        create("debugPinned") {
            val userKeystore = file(System.getProperty("user.home") + "/.android/debug.keystore")
            if (userKeystore.exists()) {
                storeFile = userKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }


    // https://stackoverflow.com/questions/52731670/android-app-bundle-with-in-app-locale-change/52733674#52733674
    bundle {
        language {
            enableSplit = false
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debugPinned")
            isShrinkResources = false
            isMinifyEnabled = false
            isDebuggable = true
            ndk {
                debugSymbolLevel = "NONE"
            }
            // NOTE: Do NOT set CHANNEL here. Build-type buildConfigFields override
            // product-flavor values in AGP, which would mask the real flavor
            // (e.g. fdroidDebug would report CHANNEL="GITHUB" instead of "FDROID",
            // hiding the fact that LiteRT stubs are in use). The flavor's own
            // CHANNEL field is authoritative.
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isShrinkResources = true
            isMinifyEnabled = true
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
    }

    flavorDimensions += "channel"
    productFlavors {
        create("github") {
            dimension = "channel"
            buildConfigField("String", "CHANNEL", "\"GITHUB\"")
        }
        create("google") {
            dimension = "channel"
            buildConfigField("String", "CHANNEL", "\"GOOGLE\"")
        }
        create("fdroid") {
            dimension = "channel"
            buildConfigField("String", "CHANNEL", "\"FDROID\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        jniLibs {
            // useLegacyPackaging = true
            excludes += listOf("META-INF/*")
            keepDebugSymbols += listOf("**/*.so")
        }
        resources {
            excludes += listOf("META-INF/*")
        }
    }
    namespace = "com.ismartcoding.plain"
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-nowarn")
    }
}
play {
    serviceAccountCredentials.set(file("play-config.json"))
    track.set("internal")
    defaultToAppBundles.set(true)
}

dependencies {
    testImplementation(libs.json)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.pdfium.android)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.okhttp)

    // Google Tink for cryptography (Ed25519 support on all Android versions)
    implementation(libs.tink.android)

    implementation(platform(libs.compose.bom))

    // https://github.com/google/accompanist/releases
    implementation(libs.compose.lifecycle.runtime)
    implementation(libs.compose.activity)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.compose.material3)
    implementation(libs.compose.components.resources)
    // https://developer.android.com/jetpack/androidx/releases/navigation
    implementation(libs.compose.navigation)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.datasource)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    implementation(libs.media3.dash)
    implementation(libs.media3.hls)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.camera.compose)

    // coil: https://coil-kt.github.io/coil/changelog/
    implementation(libs.coil)
    implementation(libs.coil.video)
    implementation(libs.coil.svg)
    implementation(libs.coil.gif)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.zxing.core)

    debugImplementation(libs.leakcanary.android)
    implementation(kotlin("stdlib", libs.versions.kotlin.get()))

    // AI Image Search: both MediaPipe and LiteRT are excluded from fdroid
    // to pass F-Droid FOSS checks (com.google.ai.edge.litert is non-FLOSS
    // after 1.2.0 — see issue #333). The shared module compiles against
    // local stubs (project(":litert-stubs")); github/google flavors provide
    // the real runtime here.
    "githubImplementation"(libs.mediapipe.tasks.vision)
    "googleImplementation"(libs.mediapipe.tasks.vision)
    "githubImplementation"(libs.litert)
    "googleImplementation"(libs.litert)
}
