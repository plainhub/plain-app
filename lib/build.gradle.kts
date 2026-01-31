plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization") version libs.versions.kotlin
}

android {
    compileSdk = 35

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += listOf("META-INF/*")
        }
    }

    namespace = "com.ismartcoding.lib"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)


    implementation(libs.androidx.lifecycle.viewmodel.ktx)

//    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation(files("libs/PdfiumAndroid-2.0.0-release.aar"))

    api(libs.gson)

    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.datetime)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.extensions)

    api(libs.androidx.appcompat)
    api(libs.androidx.core.ktx)
    api(libs.androidx.transition)

//    api(libs.exoplayer)
    // https://developer.android.com/topic/performance/graphics/load-bitmap
    api(libs.glide)
    ksp(libs.ksp)

    implementation(libs.bcprov.jdk15on)
    implementation(libs.bcpkix.jdk15on)
    api(libs.ktor.client.core)
    api(libs.ktor.client.cio)
    api(libs.ktor.client.logging)

    api(libs.markwon.core)
    api(libs.markwon.html)
    api(libs.markwon.strikethrough)
    api(libs.markwon.tasklist)
    api(libs.markwon.tables)
    api(libs.markwon.latex)
    api(libs.markwon.linkify)
    api(libs.okhttp)
    implementation(libs.android.gif.drawable)

    api(libs.jsoup)
    
    // Google Tink for cryptography (Ed25519 support on all Android versions)
    api(libs.tink.android)

    testImplementation("junit:junit:4.13.2")
}
