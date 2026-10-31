package com.ismartcoding.plain.buildtools

import java.io.File
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidRustBuildTest {
    companion object {
        private val plans: Map<String, Properties> by lazy {
            GradleTestBuild().use { build ->
                listOf("Debug", "Release").associateWith { variant ->
                    build.run(":app:assembleGithub$variant", "--dry-run")
                }
            }
        }
    }

    private fun plan(variant: String, field: String): List<String> {
        val key = if (field == "selected") field else "buildPlainRustAndroid$variant.$field"
        return checkNotNull(plans.getValue(variant).getProperty(key)).lines()
    }

    @Test
    fun testTaskCanBeCreatedWithoutReadingAnotherProjectsState() {
        GradleTestBuild().use { it.run(":shared:checkTestTaskIsolation") }
    }

    @Test
    fun debugUsesOnlyTheIncrementalNativeProfile() {
        assertEquals(listOf("buildPlainRustAndroidDebug"), plan("Debug", "selected"))
        val command = plan("Debug", "command")
        assertTrue("rustc" in command)
        assertEquals("dev", command[command.indexOf("--profile") + 1])
        assertEquals("cdylib", command[command.indexOf("--crate-type") + 1])
        assertFalse("--release" in command)
    }

    @Test
    fun debugDoesNotInheritReleaseOptimizationForSharedBusinessCode() {
        val manifest = File(buildTestRepository(), "plain-rs/Cargo.toml").readText()
        fun section(name: String): Map<String, String> {
            val lines = manifest.lineSequence().map { it.substringBefore('#').trim() }.toList()
            val header = "[$name]"
            assertTrue(header in lines, "Missing $name")
            return lines.dropWhile { it != header }.drop(1).takeWhile { !it.startsWith("[") }
                .filter { it.isNotEmpty() }.associate {
                    val parts = it.split('=', limit = 2)
                    parts[0].trim() to parts[1].trim()
                }
        }
        val dev = section("profile.dev")
        assertEquals("false", dev["lto"])
        assertEquals("true", dev["incremental"])
        assertTrue(dev.getValue("codegen-units").toInt() > 1)
        for (name in listOf("plain-server", "plain-rs")) {
            assertEquals("1", section("profile.dev.package.$name")["opt-level"])
        }
        assertEquals("3", section("profile.dev.package.\"*\"")["opt-level"])
    }

    @Test
    fun releaseKeepsTheOptimizedNativeProfile() {
        assertEquals(listOf("buildPlainRustAndroidRelease"), plan("Release", "selected"))
        val command = plan("Release", "command")
        assertEquals("release", command[command.indexOf("--profile") + 1])
        assertEquals("cdylib", command[command.indexOf("--crate-type") + 1])
    }

    @Test
    fun profilesNeverPackageEachOthersLibrary() {
        val debug = plan("Debug", "outputs")
        val release = plan("Release", "outputs")
        assertTrue(debug.any { it.endsWith("/debug/libplain_rust.so") })
        assertTrue(release.any { it.endsWith("/release/libplain_rust.so") })
        assertTrue(debug.intersect(release.toSet()).isEmpty())
    }
}
