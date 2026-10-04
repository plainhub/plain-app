package com.ismartcoding.plain.chat.peer

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

object PeerTransportPrewarmer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun prewarm(peerId: String) {
        scope.launch {
            try {
                RustContentApi.postJson("chat/prewarm", buildJsonObject { put("id", peerId) }, longRunning = true)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { LogCat.e("Peer prewarm", error) }
        }
    }
}
