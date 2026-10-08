package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.ContentApiSession
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

internal object FileResourceStreamHost {
    private val client by lazy { createPlainHttpClient(PlainHttpClientSpec.HttpHost) }
    fun start(scope: CoroutineScope, session: ContentApiSession, params: JsonObject) {
        val request = JsonHelper.jsonDecodeFromElement<ResourceStreamId>(params)
        scope.launch {
            try {
                client.webSocket(session.baseUrl.replace("http://", "ws://") + "/resources/${request.id}", session.headers()) { socket ->
                    val frame = socket.incoming.receiveCatching().getOrNull() ?: return@webSocket
                    val metadata = JsonHelper.jsonDecode<ResourceStreamRequest>(checkNotNull(frame.text))
                    FileResourceHost.stream(socket, metadata.path)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { LogCat.e("Native resource stream ended", error) }
        }
    }
}
