package com.ismartcoding.plain.platform.files


import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.rust.native.RustNativeReply
import com.ismartcoding.plain.rust.native.RustNativeBindings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

internal object FileResourceStreamHost {
    fun start(scope: CoroutineScope, params: JsonObject) {
        val id = JsonHelper.jsonDecodeFromElement<ResourceStreamId>(params).id
        scope.launch {
            var completed = false
            try {
                val metadata = JsonHelper.jsonDecode<ResourceStreamRequest>(RustNativeReply.value(RustNativeBindings.resourceOpen(id)))
                if (metadata.file) {
                    provideResourceFile(id, metadata.path)
                    completed = true
                    return@launch
                }
                val sink = NativeResourceStreamSink(id)
                try {
                    check(streamContentUri(metadata.path, sink) != null) { "Unable to open content URI" }
                    val error = RustNativeBindings.resourceClose(id, false)
                    check(error.isEmpty()) { error }
                    completed = true
                } finally { sink.close() }
            } catch (canceled: CancellationException) { throw canceled }
            catch (error: Exception) { LogCat.e("Native resource stream ended", error) }
            finally { if (!completed) RustNativeBindings.resourceClose(id, true) }
        }
    }
}
