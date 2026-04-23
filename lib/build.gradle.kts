plugins {
    id("com.android.library")
    kotlin("plugin.serialization") version libs.versions.kotlin
}

android {
    compileSdk = 36

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
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-nowarn")
        }
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

    implementation(kotlin("reflect"))
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

//    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    api(libs.pdfium.android)

    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.datetime)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.service)

    api(libs.androidx.appcompat)
    api(libs.androidx.core.ktx)
    api(libs.androidx.transition)

//    api(libs.exoplayer)

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

    api(libs.jsoup)
    
    // Google Tink for cryptography (Ed25519 support on all Android versions)
    api(libs.tink.android)

    api(libs.ktor.server.core)
    api(libs.ktor.server.netty)
    api(libs.ktor.server.websockets)
    api(libs.ktor.server.compression)
    api(libs.ktor.server.content.negotiation)
    api(libs.ktor.network.tls.certificates)
    api(libs.ktor.serialization.kotlinx.json)
    api(libs.ktor.server.caching.headers)
    api(libs.ktor.server.cors)
    api(libs.ktor.server.forwarded.header)
    api(libs.ktor.server.partial.content)
    api(libs.ktor.server.auto.head.response)
    api(libs.ktor.server.conditional.headers)

    testImplementation(libs.junit)
}
