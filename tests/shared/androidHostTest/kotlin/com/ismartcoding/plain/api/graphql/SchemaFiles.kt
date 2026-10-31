package com.ismartcoding.plain.api.graphql

import java.io.File

internal fun readMainSchema(): String {
    val directory = File(checkNotNull(System.getenv("PLAIN_CORE_DIR")), "schema/api")
    check(directory.isDirectory) { "Missing shared Rust schema directory: $directory" }
    val files = checkNotNull(directory.listFiles { file -> file.isFile && file.extension == "graphqls" }).sortedBy { it.name }
    check(files.isNotEmpty()) { "Shared Rust schema fragments are empty" }
    return files.joinToString("\n") { it.readText() }
}
