plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.devtools.ksp)
    kotlin("plugin.parcelize")
}

// Dev-only single-target mode: pass -PenableDeviceTarget=false to skip
// configuring iosArm64 (dependency resolution, KSP, task graph) when only
// building for the simulator. Saves ~3-5s of configuration overhead per
// build. Restore by omitting the flag (default: both targets enabled).
val enableDeviceTarget = (findProperty("enableDeviceTarget") as? String)?.toBoolean() ?: true

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.ismartcoding.plain.shared"
        compileSdk = 37
        minSdk = 28

        // Required so that Compose Multiplatform resources (drawable/strings/etc.)
        // are packaged into the consuming Android app's assets. Without this the
        // new `com.android.kotlin.multiplatform.library` plugin disables Android
        // resource processing and `copyAndroidMainComposeResourcesToAndroidAssets`
        // is never wired up.
        androidResources {
            enable = true
        }

        // Enable Android host-side unit tests so `commonTest` (and any
        // androidUnitTest-specific sources) get compiled and runnable via
        // `testDebugUnitTest` / `testReleaseUnitTest`. Without this the
        // plugin silently ignores both source sets and `:shared:commonTest`
        // emits a warning on every build.
        withHostTest {}
    }

    // Dev-only single-target mode: see enableDeviceTarget above.
    val iosTargets = mutableListOf<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>()
    if (enableDeviceTarget) iosTargets.add(iosArm64())
    iosTargets.add(iosSimulatorArm64())
    iosTargets.forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "PlainShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared-lib"))
            // Room database module. Exposes AppDatabase, DAOs, entities and the
            // Room runtime so the rest of :shared can reference them without
            // triggering the Room KSP here.
            api(project(":room-db"))
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            api("org.jetbrains.compose.components:components-resources:1.11.1")
            implementation(libs.navigation.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.compose.lifecycle.runtime)
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.datastore.preferences.core)
            api(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.coil.compose)

            // Vendored multiplatform-markdown-renderer (lib/markdown/) depends on these.
            api(libs.jetbrains.markdown)
            api(libs.kotlinx.collections.immutable)
            api(libs.kotlinx.coroutines.core)

            // LaTeX math rendering (KMP — Compose Multiplatform renderer)
            api(libs.latex.base)
            api(libs.latex.parser)
            api(libs.latex.renderer)
        }
        androidMain.dependencies {
            api(project(":shared-lib"))
            implementation(libs.ktor.client.okhttp)
            implementation(libs.okhttp)
            implementation(libs.tink.android)
            // kotlin-reflect provides kotlin.reflect.full (memberProperties,
            // isSubclassOf, etc.) used by the Android ReflectionBridge actual.
            // Not needed in commonMain — the stdlib provides the base
            // KClass/KProperty1/KType interfaces including KCallable.returnType.
            implementation(libs.kotlin.reflect)
            implementation(libs.androidx.exifinterface)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.coil)
            implementation(libs.coil.svg)
            implementation(libs.coil.gif)
            implementation(libs.coil.video)
            implementation(libs.coil.network.okhttp)

            // Vendored libraries (lib/) dependencies
            implementation(libs.bcprov.jdk15on)
            implementation(libs.bcpkix.jdk15on)
            implementation(libs.pdfium.android)
            implementation(libs.zxing.core)

            // Ktor server (used by vendored kgraphql and web server code)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.netty)
            implementation(libs.ktor.server.websockets)
            implementation(libs.ktor.server.compression)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.server.caching.headers)
            implementation(libs.ktor.server.cors)
            implementation(libs.ktor.server.forwarded.header)
            implementation(libs.ktor.server.partial.content)
            implementation(libs.ktor.server.auto.head.response)
            implementation(libs.ktor.server.conditional.headers)
            implementation(libs.ktor.network.tls.certificates)

            // Media3 (AudioPlayerService, audio playback, PlayerView UI)
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.datasource)
            implementation(libs.media3.session)
            implementation(libs.media3.ui)
            implementation(libs.media3.ui.compose)
            implementation(libs.media3.dash)
            implementation(libs.media3.hls)

            // LifecycleService (HttpServerService)
            implementation(libs.androidx.lifecycle.service)

            // WorkManager (FeedFetchWorker, ImageEmbedWorker)
            implementation(libs.androidx.work.runtime.ktx)

            // Splash screen (MainActivity)
            implementation(libs.androidx.core.splashscreen)

            // Activity Compose (LocalActivity)
            implementation(libs.compose.activity)

            // CameraX (ScanPage, ScanCameraView)
            implementation(libs.camera.core)
            implementation(libs.camera.camera2)
            implementation(libs.camera.lifecycle)
            implementation(libs.camera.view)
            implementation(libs.camera.compose)

            // Transitions (UI animations)
            implementation(libs.androidx.transition)

            // LiteRT (AI image search) — compileOnly stubs; real runtime provided by
            // app flavors (github/google). Declaring a local FLOSS stubs module here
            // instead of `libs.litert` keeps the non-FLOSS artifact out of the shared
            // module's dependency tree so F-Droid's scanner doesn't flag it.
            compileOnly(project(":litert-stubs"))
        }
        iosMain.dependencies {
            implementation(libs.sqlite.bundled)
            implementation(libs.ktor.client.darwin)
            implementation(libs.coil)
            implementation(libs.coil.network.ktor3)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

dependencies {
    // KGraphQL KSP2 processor — generates reflection-free schema descriptors.
    // Uses expect/actual pattern: commonMain declares expect, each platform's
    // KSP generates the actual + descriptor objects. No kspCommonMainMetadata
    // needed (platform KSP is sufficient).
    add("kspAndroid", project(":kgraphql-ksp"))
    if (enableDeviceTarget) add("kspIosArm64", project(":kgraphql-ksp"))
    add("kspIosSimulatorArm64", project(":kgraphql-ksp"))
    add("androidHostTestImplementation", kotlin("test"))
    add("androidHostTestImplementation", libs.junit)
}

compose.resources {
    packageOfResClass = "com.ismartcoding.plain.i18n"
    generateResClass = always
    publicResClass = true
}

// ============================================================================
// Compile-time validation: detect duplicate string resource keys.
//
// Compose Resources treats all strings*.xml files in the same values/ or
// values-{lang}/ directory as a SINGLE namespace. A key appearing in more than
// one file within the same locale causes a runtime crash:
//   "Resource with ID='string:X' has more than one file"
// This task fails the build at compile time so the issue is caught before any
// device/emulator run. Also detects within-file duplicates.
// ============================================================================
val checkStringResourceDuplicates by tasks.registering {
    group = "verification"
    description = "Checks for duplicate string resource keys within each locale directory."

    val resourcesDir = layout.projectDirectory.dir("src/commonMain/composeResources")
    inputs.dir(resourcesDir).withPropertyName("composeResources")

    doLast {
        val keyPattern = Regex("""<string\s+name="([^"]+)"""")
        val errors = mutableListOf<String>()

        resourcesDir.asFile.listFiles()?.sortedBy { it.name }?.forEach { localeDir ->
            if (!localeDir.isDirectory) return@forEach
            val name = localeDir.name
            if (name != "values" && !name.startsWith("values-")) return@forEach

            // key -> set of files containing it
            val keyToFiles = mutableMapOf<String, MutableMap<String, Int>>()
            localeDir.listFiles { f -> f.isFile && f.name.startsWith("strings") && f.name.endsWith(".xml") }
                ?.sortedBy { it.name }
                ?.forEach { xmlFile ->
                    val content = xmlFile.readText()
                    keyPattern.findAll(content).forEach { match ->
                        val fileMap = keyToFiles.getOrPut(match.groupValues[1]) { mutableMapOf() }
                        fileMap[xmlFile.name] = (fileMap[xmlFile.name] ?: 0) + 1
                    }
                }

            keyToFiles.forEach { (key, fileCounts) ->
                val totalOccurrences = fileCounts.values.sum()
                val distinctFiles = fileCounts.keys
                // Cross-file duplicate (the crash scenario)
                if (distinctFiles.size > 1) {
                    errors.add("  $name/ key='$key' appears in multiple files: ${distinctFiles.sorted().joinToString(", ")}")
                }
                // Within-file duplicate (same key twice in one file)
                fileCounts.forEach { (file, count) ->
                    if (count > 1) {
                        errors.add("  $name/$file key='$key' appears $count times within the same file")
                    }
                }
            }
        }

        if (errors.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Found ${errors.size} duplicate string resource key(s) in Compose Resources.")
                    appendLine("Compose Resources does not allow the same key in multiple strings*.xml files")
                    appendLine("within the same locale directory, nor the same key twice in one file.")
                    appendLine("Fix by removing the duplicate key from all but one location.")
                    appendLine()
                    errors.forEach { appendLine(it) }
                }
            )
        }
    }
}

// Run the check before Compose Resources are processed and as part of `check`.
// This ensures the build fails at compile time, not at runtime.
tasks.matching { it.name.contains("ComposeResources", ignoreCase = true) }.configureEach {
    dependsOn(checkStringResourceDuplicates)
}
tasks.named("check").configure { dependsOn(checkStringResourceDuplicates) }
