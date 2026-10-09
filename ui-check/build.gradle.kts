plugins {
    kotlin("jvm")
    application
}

kotlin { jvmToolchain(17) }
dependencies {
    implementation(libs.kotlin.compiler.embeddable)
    testImplementation(kotlin("test-junit"))
}
application { mainClass.set("com.ismartcoding.uicheck.MainKt") }
