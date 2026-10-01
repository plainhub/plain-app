plugins {
    kotlin("multiplatform")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    id("com.android.kotlin.multiplatform.library")
    id("maven-publish")
}

group = "com.ismartcoding"
version = providers.gradleProperty("plainUiVersion")
    .orElse("0.1.0-SNAPSHOT")
    .get()

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.ismartcoding.plain.ui"
        compileSdk = 37
        minSdk = 28

        // Required so that Compose Multiplatform resources (strings_components/
        // drawable) are packaged into the published AAR's assets. Without this the
        // new `com.android.kotlin.multiplatform.library` plugin disables Android
        // resource processing and `copyAndroidMainComposeResourcesToAndroidAssets`
        // is never wired up — consumers crash with MissingResourceException at
        // runtime when plain-ui components call stringResource() on their own
        // resources (seen in plain-router-app on plain-ui 0.3.0).
        androidResources {
            enable = true
        }

        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":plain-common"))
            api(libs.runtime)
            api(libs.ui)
            api(libs.foundation)
            api(libs.material3)
            api(libs.compose.components.resources)
            api(libs.coil.compose)
            api(libs.kotlinx.coroutines.core)
            api(libs.atomicfu)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            api(libs.zxing.core)
            implementation(libs.camera.core)
            implementation(libs.camera.camera2)
            implementation(libs.camera.lifecycle)
            implementation(libs.camera.view)
            implementation(libs.camera.compose)
            implementation(libs.compose.lifecycle.runtime)
        }
    }
}

compose.resources {
    packageOfResClass = "com.ismartcoding.plain.ui.resources"
    generateResClass = always
    publicResClass = true
}

dependencies {
    add("androidHostTestImplementation", kotlin("test"))
    add("androidHostTestImplementation", libs.junit)
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "plain-ui"
            description = "Reusable Compose UI components from PlainApp"
            url = "https://github.com/plainhub/plain-app"
            licenses {
                license {
                    name = "GNU Affero General Public License v3.0"
                    url = "https://www.gnu.org/licenses/agpl-3.0.html"
                }
            }
            scm {
                url = "https://github.com/plainhub/plain-app"
                connection = "scm:git:git://github.com/plainhub/plain-app.git"
                developerConnection = "scm:git:ssh://git@github.com/plainhub/plain-app.git"
            }
        }
    }
    repositories {
        maven {
            name = "plainUi"
            url = uri(layout.buildDirectory.dir("maven-repository"))
        }
    }
}
