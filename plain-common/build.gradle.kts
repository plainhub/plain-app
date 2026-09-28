plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.serialization)
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

        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.atomicfu)
            api(libs.kotlinx.serialization.json)
            api(libs.ktor.http)
            api(libs.runtime)
        }

        androidMain.dependencies {
            api(libs.androidx.core.ktx)
            api(libs.kotlin.reflect)
            api(libs.ui)
            api(libs.netty.handler)
            api(libs.netty.codec.http)
            api(libs.netty.transport.native.epoll)
            api(libs.netty.transport.native.kqueue)
            implementation(libs.androidx.exifinterface)
            implementation(libs.zxing.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

dependencies {
    add("androidHostTestImplementation", kotlin("test"))
    add("androidHostTestImplementation", libs.junit)
    add("androidHostTestImplementation", libs.kotlin.reflect)
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "plain-common"
            description = "Reusable Kotlin Multiplatform utilities and server components from PlainApp"
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
            url = uri(
                providers.gradleProperty("plainCommonMavenUrl")
                    .orElse(layout.buildDirectory.dir("maven-repository").map { it.asFile.toURI().toString() })
                    .get()
            )

            if (providers.gradleProperty("plainCommonMavenUsername").isPresent) {
                credentials {
                    username = providers.gradleProperty("plainCommonMavenUsername").get()
                    password = providers.gradleProperty("plainCommonMavenPassword").orNull
                }
            }
        }
    }
}
