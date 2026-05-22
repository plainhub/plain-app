package com.ismartcoding.plain.data

import kotlinx.serialization.Serializable

@Serializable
data class DSignatureKeyPair(
    val privateKey: String = "",  // Base64 encoded 32-byte raw Ed25519 private key
    val publicKey: String = ""    // Base64 encoded 32-byte raw Ed25519 public key
)