package com.ismartcoding.plain.lib.dlna

data class PendingCastRequest(
    val senderIp: String,
    val senderName: String,
    val mediaUri: String,
    val mediaTitle: String,
    val mediaType: DlnaMediaType = DlnaMediaType.UNKNOWN,
    val albumArtUri: String = "",
)