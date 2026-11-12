package com.ismartcoding.plain.features.nearby

import com.ismartcoding.plain.lib.withIO

object PairingResponder {
    suspend fun respond(peerId: String, accepted: Boolean) = withIO { RustPairingRuntime.respond(peerId, accepted) }
}
