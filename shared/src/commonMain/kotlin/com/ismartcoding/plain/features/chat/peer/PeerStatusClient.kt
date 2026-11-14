package com.ismartcoding.plain.features.chat.peer

import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.rust.native.RustNativeApi
import kotlinx.coroutines.CancellationException

object PeerStatusClient {
    fun reconnectNow(reason: String) {
        LogCat.d("Peer status reconnect: $reason")
        coIO {
            try {
                RustNativeApi.reconnectPeers()
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (error: Exception) {
                LogCat.e("Rust peer status reconnect", error)
            }
        }
    }
}
