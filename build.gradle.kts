// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)
        // AGP 9.5.0's default Bouncy Castle predates fixes for CVE-2026-8763 and CVE-2026-13506.
        constraints {
            for (module in listOf("bcpkix", "bcprov", "bcutil")) {
                add("classpath", "org.bouncycastle:$module-jdk18on") {
                    version { strictly("1.86") }
                }
            }
        }
    }
}

plugins {
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    tasks.withType<Exec>().configureEach {
        if (name.startsWith("buildPlainRust")) {
            workingDir(rootProject.projectDir)
            outputs.upToDateWhen { false }
        }
    }
}
