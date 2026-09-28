package com.ismartcoding.plain.platform

/**
 * Ensure the directory at [path] exists, creating it (and any missing parents)
 * if necessary.
 */
expect fun ensureDir(path: String)

/**
 * Append [line] to the text file at [path] and return the resulting file size
 * in bytes. Creates the file if it does not exist.
 */
expect fun appendLine(path: String, line: String): Long

/**
 * Delete the file at [path] if it exists.
 */
expect fun deleteFileIfExists(path: String)

/**
 * Rename a file from [from] to [to].
 */
expect fun renameFile(from: String, to: String)
