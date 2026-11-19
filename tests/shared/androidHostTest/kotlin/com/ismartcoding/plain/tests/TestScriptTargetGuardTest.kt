package com.ismartcoding.plain.tests

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TestScriptTargetGuardTest {

    private val repoRoot: File
        get() {
            var dir = File("").absoluteFile
            while (!File(dir, "settings.gradle.kts").exists()) {
                dir = dir.parentFile ?: error("could not find the repository root from ${File("").absolutePath}")
            }
            return dir
        }

    private fun scripts(): List<File> = File(repoRoot, "scripts").listFiles { f: File ->
        f.isFile && (f.name.endsWith(".sh") || f.name.endsWith(".mjs") || f.name.endsWith(".py"))
    }?.toList() ?: emptyList()

    @Test
    fun noScriptHardcodesTheAppPackage() {
        val offenders = scripts().filter { file ->
            val text = file.readText()
            Regex("""com\.ismartcoding\.plain(?:\.(?:debug|acceptance|navtest))?(?![\w.])""").containsMatchIn(text)
        }
        assertTrue(
            "these scripts name the app package literally; source test-lib.sh and use \$PACKAGE: " +
                offenders.joinToString { it.name },
            offenders.isEmpty(),
        )
    }

    @Test
    fun deviceScriptsIgnoreAlternatePackageOverrides() {
        val process = ProcessBuilder("bash", "-c", "source scripts/test-lib.sh && printf '%s' \"\$PACKAGE\"")
            .directory(repoRoot)
            .redirectErrorStream(true)
            .apply { environment()["PLAIN_PACKAGE"] = "com.example.other" }
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        assertEquals(output, 0, process.waitFor())
        assertEquals("com.ismartcoding.plain.debug", output)
    }

    @Test
    fun noScriptHardcodesTheHttpPort() {
        // 8080/8443 are the defaults, not a promise: the ports are a stored
        // preference, so a phone carrying the other build answers elsewhere.
        // Prose in a comment may still name them — the rule is about a value a
        // script would actually dial.
        val port = Regex("""\b(8080|8443)\b""")
        val offenders = scripts().filter { file ->
            file.readLines().any { line ->
                !line.trimStart().startsWith("#") && port.containsMatchIn(line)
            }
        }
        assertTrue(
            "these scripts dial a hardcoded port; read it from app_ports: " +
                offenders.joinToString { it.name },
            offenders.isEmpty(),
        )
    }

    @Test
    fun everyDeviceFacingScriptProvesTheBuildBeforeItMeasures() {
        val facing = listOf(
            "test-graphql-api.sh",
            "test-graphql-mutations.sh",
            "test-ui.sh",
            "test-api.sh",
        )
        facing.forEach { name ->
            val file = File(repoRoot, "scripts/$name")
            assertTrue("$name is missing", file.exists())
            val text = file.readText()
            assertTrue(
                "$name talks to a device without calling verify_app_under_test, " +
                    "so it can silently measure the other build on the phone",
                text.contains("verify_app_under_test"),
            )
        }
    }

    @Test
    fun everySharedHelperParsesUnderPosixSh() {
        // `PLAIN_APPROVE_CMD` is run by the API client through a POSIX `sh`
        // child, where `< <(…)` is a syntax error. That surfaced as "the gate
        // failed" rather than "the gate broke", and cost a run to read.
        val file = File(repoRoot, "scripts/test-lib.sh")
        val offenders = file.readLines()
            .withIndex()
            .filter { (_, line) ->
                !line.trimStart().startsWith("#") && Regex("""<\s*<\(""").containsMatchIn(line)
            }
            .map { (i, _) -> "test-lib.sh:${i + 1}" }
        assertTrue(
            "process substitution is not available in the sh child that runs the " +
                "approval command: ${offenders.joinToString()}",
            offenders.isEmpty(),
        )
    }
}
