package com.ismartcoding.plain.ui.components.codeeditor

import com.ismartcoding.plain.ui.components.codeeditor.engine.ByteSource

/** Platform file access used by the editor without depending on an application module. */
interface EditorFileIO {
    fun nowMillis(): Long
    fun openByteSource(path: String): ByteSource
    fun writeByteChunksStreaming(path: String, chunks: Iterator<ByteArray>): Boolean
}
