plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.devtools.ksp)
    // Resolved from the root project's buildscript classpath (libs.room.gradle).
    id("androidx.room3")
}

// Dev-only single-target mode: pass -PenableDeviceTarget=false to skip
// configuring iosArm64 (dependency resolution, KSP, task graph) when only
// building for the simulator. Saves ~3-5s of configuration overhead per
// build. Restore by omitting the flag (default: both targets enabled).
val enableDeviceTarget = (findProperty("enableDeviceTarget") as? String)?.toBoolean() ?: true

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.ismartcoding.plain.roomdb"
        compileSdk = 37
        minSdk = 28

        // Enable Android host-side unit tests so `commonTest` gets compiled.
        withHostTest {}
    }

    // Dev-only single-target mode: see enableDeviceTarget above.
    val iosTargets = mutableListOf<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>()
    if (enableDeviceTarget) iosTargets.add(iosArm64())
    iosTargets.add(iosSimulatorArm64())

    sourceSets {
        commonMain.dependencies {
            // Base domain primitives (TimeHelper, kotlinx.serialization, etc.).
            api(project(":shared-lib"))
            // Room runtime: @Entity / @Dao / @Database / @TypeConverter annotations
            // and the generated AppDatabase_Impl.
            api(libs.room.runtime)
        }
        androidMain.dependencies {
            api(project(":shared-lib"))
            // BundledSQLiteDriver used by the Android buildAppDatabase actual (Room 3
            // has no SupportSQLite fallback; both platforms run the same bundled driver).
            implementation(libs.sqlite.bundled)
        }
        iosMain.dependencies {
            // BundledSQLiteDriver used by the iOS buildAppDatabase actual.
            implementation(libs.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

room3 {
    // Room writes the schema history here; the @Database uses exportSchema = true.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Room KSP runs ONLY in this module. Changing business code in :shared no
    // longer dirties the database source set, so this task stays UP-TO-DATE.
    add("kspAndroid", libs.room.compiler)
    if (enableDeviceTarget) add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
