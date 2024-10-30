// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.room.gradle)
        classpath(libs.google.services)
        classpath(libs.firebase.crashlytics.gradle)
    }
}

plugins {
    alias(libs.plugins.devtools.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.layout.buildDirectory)
}
