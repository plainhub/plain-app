plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization") version "1.9.10"
}

android {
    compileSdk = 34

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
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
    implementation("androidx.core:core-ktx:1.12.0")
    api("androidx.appcompat:appcompat:1.6.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    val ktor = "3.0.0-beta-1"
    val markwon = "4.6.2"
    val coil = "2.4.0"

//    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation("com.github.barteksc:pdfium-android:1.9.0")

    implementation("com.google.code.gson:gson:2.10.1")

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    api("com.google.android.material:material:1.12.0-alpha01")
    api("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    api("androidx.lifecycle:lifecycle-extensions:2.2.0")

    api("androidx.fragment:fragment-ktx:1.6.2")
    api("androidx.appcompat:appcompat:1.6.1")
    api("androidx.core:core-ktx:1.12.0")
    api("androidx.transition:transition:1.4.1")
//    api("com.squareup.picasso:picasso:2.71828")

    api("com.google.android.exoplayer:exoplayer:2.19.1")
    // https://developer.android.com/topic/performance/graphics/load-bitmap
    api("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:ksp:4.16.0")
    api("com.google.android.exoplayer:exoplayer:2.19.1")
    // https://github.com/davemorrissey/subsampling-scale-image-view
    api("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")

    implementation("org.ahocorasick:ahocorasick:0.6.3") // For pinyin
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    api("io.ktor:ktor-client-core:$ktor")
    api("io.ktor:ktor-client-cio:$ktor")
    api("io.ktor:ktor-client-logging:$ktor")

    api("io.noties.markwon:core:$markwon")
    api("io.noties.markwon:html:$markwon")
    api("io.noties.markwon:ext-strikethrough:$markwon")
    api("io.noties.markwon:ext-tasklist:$markwon")
    api("io.noties.markwon:ext-tables:$markwon")
    api("io.noties.markwon:ext-latex:$markwon")
    api("io.noties.markwon:linkify:$markwon")
    api("io.noties.markwon:image-glide:$markwon")
    //api("com.caverock:androidsvg:1.4")
    api("com.caverock:androidsvg-aar:1.4")

    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.23")
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.23")

    // https://coil-kt.github.io/coil/changelog/
    api("io.coil-kt:coil:$coil")
    api("io.coil-kt:coil-compose:$coil")
    api("io.coil-kt:coil-svg:$coil")
    api("io.coil-kt:coil-gif:$coil")
    api("io.coil-kt:coil-video:$coil")
    api("com.github.Commit451.coil-transformations:transformations:2.0.2")

    api("net.dankito.readability4j:readability4j:1.0.8")
    api("org.jsoup:jsoup:1.15.3")

    api("io.github.scwang90:refresh-layout-kernel:2.0.5")
}
