package com.ismartcoding.plain.tests

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The test scripts must never name a package or a port themselves.
 *
 * A release build and a debug build live on the same phone, each holding its
 * own port, and the release one keeps running untouched. A probe aimed at the
 * wrong package answers perfectly normally, so the mistake is invisible in the
 * results — it was made once, cost a long detour into "why is the new code not
 * taking effect", and the answer was that the new build was never the thing
 * being measured. The package now comes from `app/build.gradle.kts` and the
 * ports from the build's own log, both behind `verify_app_under_test`.
 *
 * This pins the rule that keeps that from coming back through a new script.
 */
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
        // The bare applicationId, with no debug suffix. test-lib.sh derives the
        // real one from the gradle file, so a literal here can only be a copy
        // that drifts from it.
        val offenders = scripts().filter { file ->
            val text = file.readText()
            Regex("""com\.ismartcoding\.plain(?![\w.])""").containsMatchIn(text)
        }
        assertTrue(
            "these scripts name the app package literally; source test-lib.sh and use \$PACKAGE: " +
                offenders.joinToString { it.name },
            offenders.isEmpty(),
        )
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
