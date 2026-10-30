package com.ismartcoding.plain.platform.files

import java.io.InputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend fun InputStream.copyChunksTo(sink: StreamSink) {
    val buffer = ByteArray(64 * 1024)
    while (true) {
        currentCoroutineContext().ensureActive()
        val read = read(buffer)
        if (read < 0) break
        sink.write(buffer, 0, read)
    }
}
