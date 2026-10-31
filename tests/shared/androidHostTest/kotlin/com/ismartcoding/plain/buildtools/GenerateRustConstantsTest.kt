package com.ismartcoding.plain.buildtools

import com.ismartcoding.plain.lib.JsonHelper
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GenerateRustConstantsTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Serializable
    private data class CargoMetadata(val packages: List<CargoPackage>)

    @Serializable
    private data class CargoPackage(val name: String, @SerialName("manifest_path") val manifestPath: String)

    private data class Result(val exitCode: Int, val output: String, val generated: String)

    private fun generate(keys: String = "", ports: String? = null, cargoError: String? = null): Result {
        val root = temporary.newFolder()
        val source = if (ports == null) "src/features/prefs/pref_keys.rs" else "src/http/listeners/bind.rs"
        File(root, source).apply { parentFile.mkdirs() }.writeText(ports ?: keys)
        File(root, "metadata.json").writeText(JsonHelper.jsonEncode(CargoMetadata(
            listOf(CargoPackage(if (ports == null) "plain-rs" else "plain-server", File(root, "Cargo.toml").path))
        )))
        val cargoHome = File(root, "cargo-home")
        File(cargoHome, "bin").mkdirs()
        File(cargoHome, "bin/cargo").apply {
            writeText(if (cargoError == null) "#!/bin/sh\ncat \"\$(dirname \"\$0\")/../../metadata.json\"\n"
                else "#!/bin/sh\necho '$cargoError' >&2\nexit 101\n")
            check(setExecutable(true))
        }
        val output = File(root, "build/generated/" + if (ports == null) "prefKeys/PrefKeys.kt" else "httpServerPorts/HttpServerPorts.kt")
        val log = File(root, "process.log")
        val repository = buildTestRepository()
        File(root, "settings.gradle.kts").writeText("rootProject.name = \"rust-constants-test\"\n")
        File(root, "build.gradle.kts").writeText(
            "apply(from = \"${File(repository, "gradle/rust-constants.gradle.kts").invariantSeparatorsPath}\")\n",
        )
        val process = ProcessBuilder(
            File(repository, "gradlew").path, "-p", root.path, "--console=plain",
            "--no-configuration-cache", if (ports == null) "generatePrefKeys" else "generateHttpServerPorts",
        ).directory(repository).redirectErrorStream(true).redirectOutput(log).apply {
            environment()["CARGO_HOME"] = cargoHome.path
        }.start()
        if (!process.waitFor(180, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("Rust constant generation timed out")
        }
        return Result(process.exitValue(), log.readText(), output.takeIf { it.exists() }?.readText().orEmpty())
    }

    @Test
    fun cargoFailureIncludesTheOriginalDiagnostic() {
        val diagnostic = "locked Core dependency does not provide the requested feature"
        val result = generate(cargoError = diagnostic)
        assertNotEquals(0, result.exitCode)
        assertTrue(diagnostic in result.output, result.output)
        assertEquals("", result.generated)
    }

    @Test
    fun storageNamesComeDirectlyFromRust() {
        val result = generate(
            "    INTERVAL = \"interval_seconds\",\n    SORT = \"sort_key\",\n",
        )
        assertEquals(0, result.exitCode, result.output)
        for (line in listOf(
            "const val INTERVAL = \"interval_seconds\"",
            "const val SORT = \"sort_key\"",
        )) assertTrue(line in result.generated, result.generated)
    }

    @Test
    fun nativeReadKeysComeFromRustConstants() {
        val result = generate(
            "    PASSWORD = \"password\",\n" +
                "pub const UPDATE_INFO: &str = \"update_info\";\n",
        )
        assertEquals(0, result.exitCode, result.output)
        assertTrue("const val UPDATE_INFO = \"update_info\"" in result.generated, result.generated)
    }

    @Test
    fun missingOrDuplicateKeysFailGeneration() {
        for (keys in listOf("", "    A = \"same\",\n    B = \"same\",\n", "    A = \"one\",\n    A = \"two\",\n", "    A = \"same\",\npub const B: &str = \"same\";\n")) {
            val result = generate(keys)
            assertNotEquals(0, result.exitCode)
            assertTrue("Rust preference keys must be nonempty and unique" in result.output, result.output)
            assertEquals("", result.generated)
        }
    }

    @Test
    fun portChoicesPreserveRustValuesAndOrder() {
        val result = generate(ports = """
            pub const HTTP_PORTS: [u16; 2] = [9100, 9000];
            pub const HTTPS_PORTS: [u16; 2] = [9443,
                9043,];
        """.trimIndent())
        assertEquals(0, result.exitCode, result.output)
        assertEquals(
            "package com.ismartcoding.plain.network\n\n" +
                "val httpsPorts = setOf(443, 9443, 9043)\n" +
                "val httpPorts = setOf(80, 9100, 9000)\n",
            result.generated,
        )
    }

    @Test
    fun standardPortsAreNotDuplicated() {
        val result = generate(ports = """
            pub const HTTP_PORTS: [u16; 1] = [80];
            pub const HTTPS_PORTS: [u16; 1] = [443];
        """.trimIndent())
        assertEquals(0, result.exitCode, result.output)
        assertTrue("val httpPorts = setOf(80)\n" in result.generated, result.generated)
        assertTrue("val httpsPorts = setOf(443)\n" in result.generated, result.generated)
    }

    @Test
    fun invalidPortArraysFailGeneration() {
        for (http in listOf(
            "", "pub const HTTP_PORTS: [u16; 2] = [9000];",
            "pub const HTTP_PORTS: [u16; 2] = [9000, 9000];",
            "pub const HTTP_PORTS: [u16; 1] = [0];",
            "pub const HTTP_PORTS: [u16; 1] = [65536];",
            "pub const HTTP_PORTS: [u16; 1] = [9000];\n".repeat(2),
        )) {
            val result = generate(ports = "pub const HTTPS_PORTS: [u16; 1] = [9443];\n$http")
            assertNotEquals(0, result.exitCode)
            assertEquals("", result.generated)
        }
    }
}
