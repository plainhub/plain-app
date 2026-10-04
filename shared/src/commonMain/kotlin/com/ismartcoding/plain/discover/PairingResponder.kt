package com.ismartcoding.plain.discover

import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.lib.withIO

object PairingResponder {
    suspend fun respond(request: DPairingRequest, accepted: Boolean) = withIO { RustPairingRuntime.respond(request, accepted) }
    suspend fun onCancel(cancel: DPairingCancel) { RustPairingRuntime.receiveCancel(cancel) }
}
