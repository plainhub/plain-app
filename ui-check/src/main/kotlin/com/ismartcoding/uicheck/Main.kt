package com.ismartcoding.uicheck

import java.io.File

fun main(args: Array<String>) {
    require(args.isNotEmpty()) { "Pass the application repository root" }
    val root = File(args[0]).canonicalFile
    val baselineFile = File(args.getOrNull(1) ?: File(root, "scripts/ui-component-baseline.tsv").path)
    require(root.isDirectory) { "Repository not found: $root" }
    require(baselineFile.isFile) { "UI baseline not found: $baselineFile" }
    val readme = File(root, "plain-ui/README.md")
    if (readme.isFile) {
        Regex("\\]\\(([^)]+\\.kt)\\)").findAll(readme.readText()).forEach { match ->
            check(File(readme.parentFile, match.groupValues[1]).isFile) { "Broken compiled-example link: ${match.groupValues[1]}" }
        }
    }
    val findings = mutableListOf<Finding>()
    val inventory = mutableListOf<String>()
    KotlinUiCheck().use { checker ->
        listOf("shared/src", "app/src", "plain-ui/src/commonMain").forEach { sourceRoot ->
            File(root, sourceRoot).walkTopDown().filter { it.isFile && it.extension == "kt" && it.path.split(File.separatorChar).none { segment -> segment in setOf("commonTest", "androidHostTest", "androidTest", "test") } }.sortedBy { it.path }.forEach { file ->
                val path = file.relativeTo(root).invariantSeparatorsPath
                val library = path.startsWith("plain-ui/")
                val (violations, components) = checker.inspect(path, file.readText(), library)
                findings += violations
                inventory += components.map { "${it.name}\t$path:${it.line}" }
            }
        }
    }
    val counts = findings.groupingBy { it.key }.eachCount()
    val baseline = baselineFile.takeIf { it.exists() }?.readLines()?.filter { it.isNotBlank() && !it.startsWith("#") }?.associate {
        val fields = it.split('\t')
        require(fields.size == 3 && fields[2].isNotBlank()) { "Every exception needs an exact scope and reason: $it" }
        fields[0] to fields[1].toInt()
    }.orEmpty()
    val output = File(root, "build/reports/ui-check/${root.name}").apply { mkdirs() }
    File(output, "components.tsv").writeText(inventory.sorted().joinToString("\n", postfix = "\n"))
    if ("--candidates" in args) {
        File(output, "candidates.tsv").writeText(counts.toSortedMap().entries.joinToString("\n", postfix = "\n") { (key, count) ->
            val replacement = findings.first { it.key == key }.replacement
            "$key\t$count\tExisting implementation pending migration: $replacement"
        })
    }
    val failures = findings.filter { (counts[it.key] ?: 0) > (baseline[it.key] ?: 0) }
    failures.forEach { println("${root.path}/${it.key.substringBefore('|')}:${it.line}: use ${it.replacement} [${it.key.substringAfter('|')}]" ) }
    check(failures.isEmpty()) { "UI component policy: ${failures.size} violations. Do not expand the baseline to bypass the rule." }
    println("UI component policy passed for ${root.name}; ${inventory.size} public components indexed; ${findings.size} frozen legacy occurrences")
}
