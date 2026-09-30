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
