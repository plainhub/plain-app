import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.io.FileInputStream
import java.util.Properties
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.apollographql.apollo3") version "3.2.1"
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization") version "1.9.23"
}

val keystoreProperties = Properties()
rootProject.file("keystore.properties").let {
    if (it.exists()) {
        keystoreProperties.load(FileInputStream(it))
    }
}

android {
    compileSdk = 34
    defaultConfig {
        applicationId = "com.ismartcoding.plain"
        minSdk = 28
        targetSdk = 34

        val abiFilterList = if (hasProperty("abiFilters")) property("abiFilters").toString().split(';') else listOf()
        val singleAbiNum =
            when (abiFilterList.takeIf { it.size == 1 }?.first()) {
                "armeabi-v7a" -> 2
                "arm64-v8a" -> 1
                else -> 0
            }

        val vCode = 283
        versionCode = vCode - singleAbiNum
        versionName = "1.2.50"

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += abiFilterList.ifEmpty {
                listOf("arm64-v8a", "x86_64")
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
            isShrinkResources = false
            isMinifyEnabled = false
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
            buildConfigField("String", "CHANNEL", "\"\"")
//            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isShrinkResources = true
            isMinifyEnabled = true
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = true
            }
//            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
    }

    flavorDimensions += "channel"
    productFlavors {
        create("github") {
            dimension = "channel"
            buildConfigField("String", "CHANNEL", "\"GITHUB\"")
        }
        create("china") {
            dimension = "channel"
            buildConfigField("String", "CHANNEL", "\"CHINA\"")
        }
        create("google") {
            dimension = "channel"
            buildConfigField("String", "CHANNEL", "\"GOOGLE\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        dataBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.12"
    }

    packaging {
        jniLibs {
            // useLegacyPackaging = true
            excludes += listOf("META-INF/*")
        }
        resources {
            excludes += listOf("META-INF/*")
        }
    }
    namespace = "com.ismartcoding.plain"

    apollo {
        packageName.set("com.ismartcoding.plain")
        mapScalar("Time", "java.util.Date")
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(platform(libs.compose.bom))

    // https://github.com/google/accompanist/releases
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.accompanist.drawablepainter)
//    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0-alpha12")

    // https://developer.android.com/jetpack/androidx/releases/navigation
    implementation(libs.androidx.navigation.compose)

    releaseImplementation(platform(libs.firebase.bom))
    releaseImplementation(libs.firebase.crashlytics.ktx)

    // Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.cast)

    implementation(libs.apollo.runtime)
    implementation(libs.apollo.normalized.cache)
    implementation(libs.apollo.normalized.cache.sqlite)
    implementation(libs.apollo.adapters)

    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.preference.ktx)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.snake.yaml)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.network.tls.certificates)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.caching.headers)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.ktor.server.partial.content)
    implementation(libs.ktor.server.auto.head.response)
    implementation(libs.ktor.server.conditional.headers)

    implementation(libs.kgraphql)
    implementation(libs.kgraphql.ktor)

    // https://developer.android.com/jetpack/androidx/releases/room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    ksp(libs.room.compiler)

    implementation(libs.openai.client)

    implementation(libs.core)

    implementation(libs.androidx.work.runtime.ktx)

    // https://developer.android.com/jetpack/androidx/releases/datastore
    implementation(libs.androidx.datastore.preferences)

//    implementation("org.eclipse.jgit:org.eclipse.jgit:6.1.0.202203080745-r") // TODO: git support
    implementation(libs.zt.zip)
    implementation(project(":lib"))
    debugImplementation(libs.leakcanary.android)
    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))
}
