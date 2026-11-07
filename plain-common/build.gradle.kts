plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    id("com.android.kotlin.multiplatform.library")
    id("maven-publish")
}

group = "com.ismartcoding"
version = providers.gradleProperty("plainCommonVersion")
    .orElse("0.1.0-SNAPSHOT")
    .get()

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.ismartcoding.plain.common"
        compileSdk = 37
        minSdk = 28

        androidResources {
            enable = true
        }

        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        configureEach {
            if (name.endsWith("Test")) {
                kotlin.setSrcDirs(listOf(rootProject.file("tests/plain-common/$name/kotlin")))
                resources.setSrcDirs(listOf(rootProject.file("tests/plain-common/$name/resources")))
            }
        }
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.atomicfu)
            api(libs.kotlinx.serialization.json)
            api(libs.runtime)
            api(libs.ui)
            api(libs.foundation)
            api(libs.material3)
            api(libs.compose.components.resources)
            api(libs.navigation.compose)
            api(libs.coil.compose)
            implementation(libs.kotlinx.datetime)
        }

        androidMain.dependencies {
            api(libs.androidx.core.ktx)
            implementation(libs.androidx.exifinterface)
            api(libs.zxing.core)
            implementation(libs.camera.core)
            implementation(libs.camera.camera2)
            implementation(libs.camera.lifecycle)
            implementation(libs.camera.compose)
            implementation(libs.compose.lifecycle.runtime)
        }

        getByName("commonTest").kotlin.srcDir("../tests/plain-common/testFixtures/kotlin")
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
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
    add("androidHostTestImplementation", libs.kotlinx.coroutines.test)
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "plain-common"
            description = "Reusable Kotlin Multiplatform utilities and Compose UI components from PlainApp"
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
            name = "plainCommon"
            url = uri(layout.buildDirectory.dir("maven-repository"))
        }
    }
}
