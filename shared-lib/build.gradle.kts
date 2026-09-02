plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.ismartcoding.plain.lib"
        compileSdk = 37
        minSdk = 28

        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
            api(libs.ktor.http)

            // Compose Multiplatform — required by vendored markdown renderer
            api(libs.runtime)
            api(libs.ui)
            api(libs.foundation)
            api(libs.material3)
            api(libs.navigation.compose)

            // IntelliJ Markdown — the parser/AST used by the renderer
            api(libs.jetbrains.markdown)
            api(libs.kotlinx.collections.immutable)

            // LaTeX math rendering
            api(libs.latex.base)
            api(libs.latex.parser)
            api(libs.latex.renderer)

            // Coil 3 — image loading for markdown images
            api(libs.coil)
            api(libs.coil.compose)
        }

        androidMain.dependencies {
            api(libs.androidx.core.ktx)
            api(libs.kotlin.reflect)
            api(libs.atomicfu)
            api(libs.netty.handler)
            api(libs.netty.codec.http)
            api(libs.netty.transport.native.epoll)
            api(libs.netty.transport.native.kqueue)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

dependencies {
    add("androidHostTestImplementation", kotlin("test"))
    add("androidHostTestImplementation", libs.junit)
}

// Fails the build if androidMain sources call Java 21 SequencedCollection/SequencedMap
// members (removeLast/getFirst/reversed/...). Those names bind to JDK members when the
// receiver is a java.util concrete type; they only exist on API 35+ while minSdk is 28,
// causing NoSuchMethodError at runtime. Use removeAt(index), first()/last(),
// add(index, e), asReversed() instead.
val checkJava21Collisions by tasks.registering {
    group = "verification"
    description = "Rejects Java 21 collection member calls in androidMain (unavailable below API 35)"
    val srcRoot = layout.projectDirectory.dir("src/androidMain")
    inputs.dir(srcRoot)
    doLast {
        val forbidden = Regex(
            """\.(removeLast|removeFirst|getFirst|getLast|addFirst|addLast|reversed|putFirst|putLast|firstEntry|lastEntry|pollFirstEntry|pollLastEntry|sequencedKeySet|sequencedValues|sequencedEntry)\("""
        )
        val offenders = mutableListOf<String>()
        srcRoot.asFile.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { f ->
                f.readLines().forEachIndexed { i, line ->
                    // LockFreeLinkedListHead is a kotlinx-coroutines class with its own addLast member
                    if (line.contains("LockFreeLinkedListHead")) return@forEachIndexed
                    if (forbidden.containsMatchIn(line)) offenders += "${f.relativeTo(srcRoot.asFile)}:${i + 1}: ${line.trim()}"
                }
            }
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "Java 21-only collection calls found (API 35+, minSdk is 28):\n" +
                    offenders.joinToString("\n") +
                    "\nUse removeAt(index) / first() / last() / add(0, e) / asReversed() instead."
            )
        }
    }
}

tasks.matching {
    it.name.startsWith("compile") && it.name.contains("Android", ignoreCase = true)
}.configureEach { dependsOn(checkJava21Collisions) }
