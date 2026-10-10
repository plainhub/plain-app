package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.preferences.RustSystemState

object SignatureHelper {
    suspend fun getRawPublicKeyBase64Async(): String = RustSystemState.state.value.signaturePublicKey
}
