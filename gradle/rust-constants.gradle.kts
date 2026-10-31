import groovy.json.JsonSlurper

val cargo = File(System.getenv("CARGO_HOME") ?: "${System.getProperty("user.home")}/.cargo", "bin/cargo")
val cargoMetadata = providers.exec {
    workingDir(rootDir)
    isIgnoreExitValue = true
    commandLine(cargo, "metadata", "--locked", "--filter-platform", "aarch64-linux-android",
        "--format-version", "1", "--manifest-path", rootProject.file("plain-rs/Cargo.toml"))
}.let { output ->
    output.result.map { result ->
        check(result.exitValue == 0) { output.standardError.asText.get() }
        output.standardOutput.asText.get()
    }
}

fun rustPackage(metadata: String, name: String): File {
    val document = JsonSlurper().parseText(metadata) as Map<*, *>
    val packages = (document["packages"] as List<*>).map { it as Map<*, *> }
        .filter { it["name"] == name }
    require(packages.size == 1) { "Expected exactly one resolved $name dependency" }
    return File(packages.single()["manifest_path"] as String).parentFile
}

fun preferenceKeys(source: String): String {
    val keys = Regex("""^\s+([A-Z][A-Z_0-9]*) = "([a-z_0-9]+)",$""", RegexOption.MULTILINE)
        .findAll(source).map { it.groupValues[1] to it.groupValues[2] }.toList() +
        Regex("""^pub const ([A-Z][A-Z_0-9]*): &str = "([a-z_0-9]+)";$""", RegexOption.MULTILINE)
            .findAll(source).map { it.groupValues[1] to it.groupValues[2] }.toList()
    require(keys.isNotEmpty() && keys.map { it.first }.distinct().size == keys.size &&
        keys.map { it.second }.distinct().size == keys.size) {
        "Rust preference keys must be nonempty and unique"
    }
    return buildString {
        append("package com.ismartcoding.plain.preferences\n\ninternal object PrefKeys {\n")
        keys.forEach { (name, value) -> append("    const val $name = \"$value\"\n") }
        append("}\n")
    }
}

fun httpServerPorts(source: String): String = buildString {
    append("package com.ismartcoding.plain.network\n\n")
    for ((scheme, default) in listOf("https" to 443, "http" to 80)) {
        val name = "${scheme.uppercase(java.util.Locale.ROOT)}_PORTS"
        val matches = Regex("""pub const $name:\s*\[u16;\s*(\d+)\s*\]\s*=\s*\[([\d,\s]+)\];""")
            .findAll(source).toList()
        require(matches.size == 1) { "Expected exactly one Rust $name array" }
        val match = matches.single()
        val ports = match.groupValues[2].split(',').map { it.trim() }
            .filter { it.isNotEmpty() }.map { it.toInt() }
        require(ports.isNotEmpty() && ports.size == match.groupValues[1].toInt() &&
            ports.distinct().size == ports.size && ports.all { it in 1..65535 }) {
            "Invalid Rust $name array"
        }
        append("val ${scheme}Ports = setOf(${(listOf(default) + ports).distinct().joinToString(", ")})\n")
    }
}

fun registerRustConstants(taskName: String, packageName: String, sourcePath: String,
    outputPath: String, render: (String) -> String) = tasks.register(taskName) {
    val output = layout.buildDirectory.file("generated/$outputPath")
    outputs.dir(output.map { it.asFile.parentFile })
    outputs.upToDateWhen { false }
    doLast {
        val source = rustPackage(cargoMetadata.get(), packageName).resolve(sourcePath).readText()
        val content = render(source)
        val file = output.get().asFile
        if (!file.exists() || file.readText() != content) {
            file.parentFile.mkdirs()
            file.writeText(content)
        }
    }
}

registerRustConstants("generatePrefKeys", "plain-rs", "src/features/prefs/pref_keys.rs",
    "prefKeys/PrefKeys.kt", ::preferenceKeys)
registerRustConstants("generateHttpServerPorts", "plain-server", "src/http/listeners/bind.rs",
    "httpServerPorts/HttpServerPorts.kt", ::httpServerPorts)
