package com.ismartcoding.plain.platform.files

import com.ismartcoding.plain.features.files.FilesClient
import com.ismartcoding.plain.ui.components.codeeditor.engine.ByteSource
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CancellationException

expect fun openByteSource(path: String): ByteSource

suspend fun writeByteChunksStreaming(path: String, chunks: Iterator<ByteArray>): Boolean = try {
    FilesClient.writeChunks(path, chunks)
} catch (canceled: CancellationException) {
    throw canceled
} catch (error: Exception) {
    LogCat.e("File save failed", error)
    false
}
