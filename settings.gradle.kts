plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "PlainApp"
include(":app")
include(":shared")
include(":litert-stubs")
include(":kgraphql-ksp")
