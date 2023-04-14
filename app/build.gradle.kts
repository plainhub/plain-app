import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.apollographql.apollo3") version "3.2.1"
    id("kotlin-parcelize")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization") version "1.7.0"
}

val keystoreProperties = Properties()
rootProject.file("keystore.properties").let {
    if (it.exists()) {
        keystoreProperties.load(FileInputStream(it))
    }
}

android {
    compileSdk = 33
    defaultConfig {
        applicationId = "com.ismartcoding.plain"
        minSdk = 28
        targetSdk = 33
        versionCode = 14
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
            storeFile = file(keystoreProperties.getProperty("storeFile", "release.jks"))
            storePassword = keystoreProperties.getProperty("storePassword")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isShrinkResources = false
            isMinifyEnabled = false
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
//        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    packagingOptions {
        jniLibs {
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

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_11.toString()
        }
    }
}

dependencies {
    val roomVersion = "2.5.0"
    val apolloVersion = "3.2.1"
    val kgraphqlVersion = "0.18.1"
    val ktorVersion = "2.0.3"

    implementation(platform("com.google.firebase:firebase-bom:31.2.1"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    implementation("com.apollographql.apollo3:apollo-runtime:$apolloVersion")
    implementation("com.apollographql.apollo3:apollo-normalized-cache:$apolloVersion")
    implementation("com.apollographql.apollo3:apollo-normalized-cache-sqlite:$apolloVersion")
    implementation("com.apollographql.apollo3:apollo-adapters:$apolloVersion")

    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.paging:paging-runtime-ktx:3.1.1")
    implementation("androidx.preference:preference-ktx:1.2.0")

    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation("com.github.bmoliveira:snake-yaml:v1.18-android")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.0-alpha04")
    implementation("androidx.camera:camera-lifecycle:1.3.0-alpha04")
    implementation("androidx.camera:camera-view:1.3.0-alpha04")
    implementation("com.google.mlkit:barcode-scanning:17.0.3")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-compression:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-network-tls-certificates:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-caching-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-forwarded-header:$ktorVersion")
    implementation("io.ktor:ktor-server-partial-content:$ktorVersion")
    implementation("io.ktor:ktor-server-auto-head-response:$ktorVersion")
    implementation("io.ktor:ktor-server-conditional-headers:$ktorVersion")

    implementation("com.apurebase:kgraphql:$kgraphqlVersion")
    implementation("com.apurebase:kgraphql-ktor:$kgraphqlVersion")

    // https://developer.android.com/jetpack/androidx/releases/room
    implementation("androidx.room:room-common:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("com.github.skydoves:balloon:1.5.2")

    implementation("com.aallam.openai:openai-client:3.2.0")

    // Feed
    implementation("com.rometools:rome:1.18.0")
    implementation("com.rometools:rome-opml:1.18.0")

    implementation("androidx.work:work-runtime-ktx:2.8.0")

//    implementation("org.eclipse.jgit:org.eclipse.jgit:6.1.0.202203080745-r") // TODO: git support
    implementation("com.github.bumptech.glide:recyclerview-integration:4.11.0")
    implementation("org.zeroturnaround:zt-zip:1.15")
    implementation(project(":lib"))
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.9.1")
    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))
}
