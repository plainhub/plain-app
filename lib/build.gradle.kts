plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization") version "1.7.0"
}

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packagingOptions {
        resources {
            excludes += listOf("META-INF/*")
        }
    }

    namespace = "com.ismartcoding.lib"
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    api("androidx.appcompat:appcompat:1.6.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    val ktorVersion = "2.3.1"
    val markwonVersion = "4.6.2"

//    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation("com.github.barteksc:pdfium-android:1.9.0")

    implementation("com.google.code.gson:gson:2.9.1")

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    api("com.google.android.material:material:1.9.0-alpha02")
    api("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    api("androidx.lifecycle:lifecycle-extensions:2.2.0")

    api("androidx.fragment:fragment-ktx:1.5.7")
    api("androidx.appcompat:appcompat:1.6.1")
    api("androidx.core:core-ktx:1.10.1")
    api("androidx.transition:transition:1.4.1")
//    api("com.squareup.picasso:picasso:2.71828")

    api("com.github.bumptech.glide:glide:4.15.1")
    kapt("com.github.bumptech.glide:compiler:4.15.1")
    api("com.google.android.exoplayer:exoplayer:2.18.7")
    // https://github.com/davemorrissey/subsampling-scale-image-view
    api("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")

    api("com.tencent:mmkv:1.3.0")

    api("com.geyifeng.immersionbar:immersionbar:3.2.2")

    implementation("org.ahocorasick:ahocorasick:0.6.3") // For pinyin
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    api("io.ktor:ktor-client-core:$ktorVersion")
    api("io.ktor:ktor-client-cio:$ktorVersion")
    api("io.ktor:ktor-client-logging:$ktorVersion")

    api("io.noties.markwon:core:$markwonVersion")
    api("io.noties.markwon:html:$markwonVersion")
    api("io.noties.markwon:ext-strikethrough:$markwonVersion")
    api("io.noties.markwon:ext-tasklist:$markwonVersion")
    api("io.noties.markwon:ext-tables:$markwonVersion")
    api("io.noties.markwon:ext-latex:$markwonVersion")
    api("io.noties.markwon:image:$markwonVersion")
    api("io.noties.markwon:image-glide:$markwonVersion")
    api("io.noties.markwon:linkify:$markwonVersion")

    api("net.dankito.readability4j:readability4j:1.0.8")
    api("org.jsoup:jsoup:1.15.3")

    api("io.github.scwang90:refresh-layout-kernel:2.0.5")
}