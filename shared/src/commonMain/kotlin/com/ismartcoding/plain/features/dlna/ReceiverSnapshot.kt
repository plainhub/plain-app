package com.ismartcoding.plain.features.dlna

import com.ismartcoding.plain.lib.dlna.DlnaMediaType
import com.ismartcoding.plain.lib.dlna.PendingCastRequest
import kotlinx.serialization.Serializable

@Serializable
internal data class ReceiverSnapshot(
    val version: Long,
    val isRunning: Boolean, val isRetrying: Boolean, val mediaUri: String, val mediaTitle: String,
    val mediaAlbumArtUri: String, val mediaType: DlnaMediaType, val playbackState: DlnaPlaybackState,
    val port: Int, val currentPositionMs: Long, val durationMs: Long, val seekTargetMs: Long?,
    val pendingCastRequest: PendingCastRequest?, val startError: String,
)
