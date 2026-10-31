import java.io.File
import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import javax.inject.Inject
import org.gradle.api.provider.Property

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
        testInstrumentationRunner = providers.gradleProperty("testInstrumentationRunner")
            .orElse("androidx.test.runner.AndroidJUnitRunner").get()

        val abiFilterList = if (hasProperty("abiFilters")) property("abiFilters").toString().split(';') else listOf()
        val singleAbiNum =
            when (abiFilterList.takeIf { it.size == 1 }?.first()) {
                "armeabi-v7a" -> 2
                "arm64-v8a" -> 1
                else -> 0
            }

        val vCode = 706
        versionCode = vCode - singleAbiNum
        versionName = "4.0.0"

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
            applicationIdSuffix = if (providers.gradleProperty("plainAcceptance").orNull == "true") ".acceptance" else ".debug"
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
            // hiding the selected distribution channel). The flavor's own
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
            excludes += listOf("META-INF/*", "web/**")
        }
    }
    sourceSets.getByName("androidTest") {
        java.setSrcDirs(listOf("../tests/app/androidTest/java"))
        kotlin.setSrcDirs(listOf("../tests/app/androidTest/kotlin", "../tests/app/androidTest/java"))
        resources.setSrcDirs(listOf("../tests/app/androidTest/resources"))
    }
    sourceSets.getByName("androidTest").kotlin.srcDirs(
        "../tests/shared/testFixtures/kotlin",
        "../tests/plain-common/testFixtures/kotlin",
        "../tests/shared/androidTestFixtures/kotlin",
    )
    if (providers.gradleProperty("plainUiTest").orNull == "true") {
        sourceSets.getByName("androidTest").kotlin.srcDir("../../plainapp-test/ui-test/android")
    }
    if (providers.gradleProperty("plainAcceptance").orNull == "true") {
        sourceSets.getByName("androidTest").kotlin.srcDir("../../plainapp-test/android/src/androidTest/kotlin")
        sourceSets.getByName("androidTest").manifest.srcFile("../../plainapp-test/android/src/androidTest/AndroidManifest.xml")
        sourceSets.getByName("androidTest").java.srcDir("../../plainapp-test/android/src/androidTest/java")
    }
    if (providers.gradleProperty("plainRemoteManagementTest").orNull == "true") {
        val generatedTests = layout.buildDirectory.dir("generated/remote-management-tests")
        val prepareTests = tasks.register<Sync>("prepareRemoteManagementTestSource") {
            from(rootProject.file("tests/app/androidTest/kotlin")) {
                include("**/ManagedDeviceUiAcceptanceTest.kt")
            }
            into(generatedTests)
        }
        sourceSets.getByName("androidTest").java.setSrcDirs(emptyList<String>())
        sourceSets.getByName("androidTest").kotlin.setSrcDirs(listOf(generatedTests.get().asFile))
        tasks.matching { it.name.contains("AndroidTest") }.configureEach { dependsOn(prepareTests) }
    }
    namespace = "com.ismartcoding.plain"
}

val webBundleDir = providers.gradleProperty("plainWebDist")
    .orElse(providers.environmentVariable("PLAIN_WEB_DIST"))
    .orNull?.let { rootProject.file(it) }

val rustCoreDir = rootProject.file("plain-rs")
val localCoreOverride = rootProject.file(".cargo/core-dependency-state").exists()
val rustTargetDir = layout.buildDirectory.dir("rust-target")
val rustAbi = providers.gradleProperty("abiFilters").orNull?.split(';')?.singleOrNull() ?: "arm64-v8a"
val rustAndroidTarget = when (rustAbi) {
    "arm64-v8a" -> "aarch64-linux-android"
    "armeabi-v7a" -> "armv7-linux-androideabi"
    "x86_64" -> "x86_64-linux-android"
    "x86" -> "i686-linux-android"
    else -> error("Unsupported Rust Android ABI: $rustAbi")
}
val localSdk = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}.getProperty("sdk.dir")
val androidSdk = File(System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT") ?: localSdk ?: "${System.getProperty("user.home")}/Library/Android/sdk")
val androidNdk = System.getenv("ANDROID_NDK_HOME")?.let(::File)
    ?: File(androidSdk, "ndk").listFiles()?.filter { it.isDirectory }?.maxByOrNull { it.name }
    ?: error("Android NDK is required to build Rust preferences")
val ndkHost = File(androidNdk, "toolchains/llvm/prebuilt").listFiles()?.firstOrNull { it.isDirectory }
    ?: error("Android NDK LLVM toolchain was not found")
val clangTarget = when (rustAndroidTarget) {
    "armv7-linux-androideabi" -> "armv7a-linux-androideabi"
    "i686-linux-android" -> "i686-linux-android"
    else -> rustAndroidTarget
}
val androidClang = File(ndkHost, "bin/${clangTarget}28-clang")
val libcxxTarget = if (rustAbi == "armeabi-v7a") "arm-linux-androideabi" else rustAndroidTarget

// The Gradle daemon inherits the PATH of whatever shell first started it, which
// often lacks ~/.cargo/bin, and the daemon cannot fix that after startup. Call
// rustup/cargo by absolute path so Exec can start them.
val cargoBinDir = File(System.getenv("CARGO_HOME") ?: "${System.getProperty("user.home")}/.cargo", "bin")
val rustupExe = cargoBinDir.resolve("rustup")
val cargoExe = cargoBinDir.resolve("cargo")

val installRustAndroidTarget = tasks.register<Exec>("installRustAndroidTarget") {
    commandLine(rustupExe.absolutePath, "target", "add", rustAndroidTarget)
}

abstract class PackageNativeFiles @Inject constructor(
    private val fileSystem: FileSystemOperations,
) : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val nativeFiles: ConfigurableFileCollection

    @get:Input
    abstract val relativeDirectory: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun packageFiles() {
        fileSystem.sync {
            from(nativeFiles) { into(relativeDirectory.get()) }
            into(outputDirectory)
        }
    }
}

val rustPackages = listOf("debug" to "dev", "release" to "release").associate { (buildType, profile) ->
    val suffix = buildType.replaceFirstChar { it.uppercase() }
    val outputProfile = if (profile == "dev") "debug" else profile
    val runtimeDirectory = layout.buildDirectory.dir("onnxruntime/$buildType")
    val library = rustTargetDir.map { it.file("$rustAndroidTarget/$outputProfile/libplain_rust.so") }
    val buildRust = tasks.register<Exec>("buildPlainRustAndroid$suffix") {
        dependsOn(installRustAndroidTarget)
        inputs.file(File(rustCoreDir, "Cargo.toml"))
        inputs.file(File(rustCoreDir, "Cargo.lock"))
        inputs.dir(File(rustCoreDir, "src"))
        inputs.property("rustProfile", profile)
        inputs.property("rustTarget", rustAndroidTarget)
        outputs.file(library)
        outputs.file(runtimeDirectory.map { it.file("libonnxruntime.so") })
        outputs.file(runtimeDirectory.map { it.file("LICENSE") })
        outputs.file(runtimeDirectory.map { it.file("ThirdPartyNotices.txt") })
        outputs.upToDateWhen { false }
        webBundleDir?.let {
            inputs.dir(it).withPropertyName("webBundle")
            environment("PLAIN_WEB_DIST", it.absolutePath)
        }
        environment("CARGO_TARGET_DIR", rustTargetDir.get().asFile.absolutePath)
        environment("ANDROID_HOME", androidSdk.absolutePath)
        environment("ANDROID_NDK_HOME", androidNdk.absolutePath)
        environment("PLAIN_ONNX_RUNTIME_DIR", runtimeDirectory.get().asFile.absolutePath)
        environment("CARGO_TARGET_${rustAndroidTarget.uppercase().replace('-', '_')}_LINKER", androidClang.absolutePath)
        environment("CC_${rustAndroidTarget.replace('-', '_')}", androidClang.absolutePath)
        environment("AR_${rustAndroidTarget.replace('-', '_')}", File(ndkHost, "bin/llvm-ar").absolutePath)
        environment("CARGO_TARGET_${rustAndroidTarget.uppercase().replace('-', '_')}_AR", File(ndkHost, "bin/llvm-ar").absolutePath)
        commandLine(listOfNotNull(cargoExe.absolutePath, "rustc", "--locked".takeUnless { localCoreOverride },
            "--manifest-path", File(rustCoreDir, "Cargo.toml"), "--lib", "--crate-type", "cdylib",
            "--profile", profile, "--target", rustAndroidTarget))
    }
    val packageRust = tasks.register<PackageNativeFiles>("packagePlainRustAndroid$suffix") {
        dependsOn(buildRust)
        nativeFiles.from(library, runtimeDirectory.map { it.file("libonnxruntime.so") },
            File(ndkHost, "sysroot/usr/lib/$libcxxTarget/libc++_shared.so"))
        relativeDirectory.set(rustAbi)
        outputDirectory.set(layout.buildDirectory.dir("generated/rustJniLibs/$buildType"))
    }
    val packageLicenses = tasks.register<PackageNativeFiles>("packageOnnxRuntimeLicenses$suffix") {
        dependsOn(buildRust)
        nativeFiles.from(runtimeDirectory.map { it.file("LICENSE") }, runtimeDirectory.map { it.file("ThirdPartyNotices.txt") })
        relativeDirectory.set("onnxruntime")
        outputDirectory.set(layout.buildDirectory.dir("generated/onnxLicenses/$buildType"))
    }
    buildType to (packageRust to packageLicenses)
}
androidComponents.onVariants { variant ->
    val (nativePackage, licensePackage) = rustPackages.getValue(requireNotNull(variant.buildType))
    variant.sources.jniLibs?.addGeneratedSourceDirectory(nativePackage, PackageNativeFiles::outputDirectory)
    variant.sources.assets?.addGeneratedSourceDirectory(licensePackage, PackageNativeFiles::outputDirectory)
}

tasks.withType<KotlinCompile>().configureEach {
    if (name.endsWith("AndroidTestKotlin")) {
        dependsOn(":shared:bundleAndroidMainClassesToCompileJar")
        compilerOptions.freeCompilerArgs.add(provider {
            val classes = project(":shared").tasks.getByName("bundleAndroidMainClassesToCompileJar").outputs.files
            "-Xfriend-paths=" + classes.files.filter { it.extension == "jar" }.joinToString(File.pathSeparator) { it.absolutePath }
        })
    }
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
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.okhttp)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(libs.tink.android)

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

    debugImplementation(libs.leakcanary.android)
    debugImplementation(libs.androidx.work.multiprocess)
    implementation(kotlin("stdlib", libs.versions.kotlin.get()))

}
