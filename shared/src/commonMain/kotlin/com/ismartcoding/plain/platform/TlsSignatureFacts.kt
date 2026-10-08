package com.ismartcoding.plain.platform

import kotlinx.serialization.Serializable

@Serializable
internal data class TlsSignatureFacts(val signature: List<Int>)
