package com.ismartcoding.plain.features.dlna.sender

import kotlinx.serialization.Serializable

@Serializable
internal data class CastSnapshot(
    val version: Long = 0,
    val devices: List<DlnaDevice> = emptyList(), val currentDevice: DlnaDevice? = null,
    val items: List<CastItem> = emptyList(), val currentUri: String = "", val playing: Boolean = false,
    val progressMs: Long = 0, val durationMs: Long = 0, val supportsCallback: Boolean = false,
    val active: Boolean = false, val sid: String = "",
)
