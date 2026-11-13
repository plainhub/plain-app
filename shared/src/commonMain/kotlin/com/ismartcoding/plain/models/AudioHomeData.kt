package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class AudioHomeData(val audioArtists: List<AudioArtist>, val mediaPlayHistory: List<AudioHomeHistoryItem>)

@Serializable
internal data class AudioHomeHistoryItem(val media: JsonObject)
