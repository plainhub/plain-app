package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
internal data class PeerSubscriptionTarget(val peerId: String, val publicKey: String)
