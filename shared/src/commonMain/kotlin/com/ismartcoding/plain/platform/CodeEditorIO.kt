package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.codeeditor.ByteSource

/** Opens a positioned-read byte source for editor loading (plain path, file:// or content://). */
expect fun openByteSource(path: String): ByteSource

/**
 * Streams chunks to [path] with an atomic replace (temp file + rename) for plain paths.
 * Returns false on failure.
 */
expect fun writeByteChunksStreaming(path: String, chunks: Iterator<ByteArray>): Boolean
