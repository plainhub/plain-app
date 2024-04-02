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
    kotlin("plugin.serialization") version "1.9.22"
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

        val vCode = 262
        versionCode = vCode - singleAbiNum
        versionName = "1.2.43"

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
        kotlinCompilerExtensionVersion = "1.5.8"
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
    val room = "2.6.1"
    val apollo = "3.2.1"
    val kgraphql = "0.19.0"
    val ktor = "3.0.0-beta-1"
    val media3 = "1.3.0"
    val compose = "1.6.4"

    implementation(platform("androidx.compose:compose-bom:2024.01.00"))

    // https://github.com/google/accompanist/releases
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.runtime:runtime:$compose")
    implementation("androidx.compose.ui:ui:$compose")
    implementation("androidx.compose.foundation:foundation:$compose")
    implementation("androidx.compose.foundation:foundation-layout:$compose")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.0-alpha05")
    implementation("com.google.accompanist:accompanist-drawablepainter:0.34.0")
//    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0-alpha12")

    // https://developer.android.com/jetpack/androidx/releases/navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    releaseImplementation(platform("com.google.firebase:firebase-bom:32.2.3"))
    releaseImplementation("com.google.firebase:firebase-crashlytics-ktx:18.6.3")

    // Media3
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-datasource:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    implementation("androidx.media3:media3-session:$media3")
    implementation("androidx.media3:media3-cast:$media3")

    implementation("com.apollographql.apollo3:apollo-runtime:$apollo")
    implementation("com.apollographql.apollo3:apollo-normalized-cache:$apollo")
    implementation("com.apollographql.apollo3:apollo-normalized-cache-sqlite:$apollo")
    implementation("com.apollographql.apollo3:apollo-adapters:$apollo")

    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.paging:paging-runtime-ktx:3.2.1")
    implementation("androidx.preference:preference-ktx:1.2.1")

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.github.bmoliveira:snake-yaml:v1.18-android")

    // CameraX
    implementation("androidx.camera:camera-core:1.4.0-alpha04")
    implementation("androidx.camera:camera-camera2:1.4.0-alpha04")
    implementation("androidx.camera:camera-lifecycle:1.4.0-alpha04")
    implementation("androidx.camera:camera-view:1.4.0-alpha04")

    implementation("io.ktor:ktor-server-core:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-server-websockets:$ktor")
    implementation("io.ktor:ktor-server-compression:$ktor")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor")
    implementation("io.ktor:ktor-network-tls-certificates:$ktor")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
    implementation("io.ktor:ktor-server-caching-headers:$ktor")
    implementation("io.ktor:ktor-server-cors:$ktor")
    implementation("io.ktor:ktor-server-forwarded-header:$ktor")
    implementation("io.ktor:ktor-server-partial-content:$ktor")
    implementation("io.ktor:ktor-server-auto-head-response:$ktor")
    implementation("io.ktor:ktor-server-conditional-headers:$ktor")

    implementation("com.apurebase:kgraphql:$kgraphql")
    implementation("com.apurebase:kgraphql-ktor:$kgraphql")

    // https://developer.android.com/jetpack/androidx/releases/room
    implementation("androidx.room:room-common:$room")
    ksp("androidx.room:room-compiler:$room")
    implementation("androidx.room:room-ktx:$room")
    // implementation("com.github.skydoves:balloon:1.5.2")

    implementation("com.aallam.openai:openai-client:3.6.2")

    implementation("com.google.zxing:core:3.5.3")

    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // https://developer.android.com/jetpack/androidx/releases/datastore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

//    implementation("org.eclipse.jgit:org.eclipse.jgit:6.1.0.202203080745-r") // TODO: git support
    implementation("org.zeroturnaround:zt-zip:1.16")
    implementation(project(":lib"))
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.9.1")
    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))
}
