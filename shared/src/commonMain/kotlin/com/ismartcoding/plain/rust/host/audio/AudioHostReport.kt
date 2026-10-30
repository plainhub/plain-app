package com.ismartcoding.plain.rust.host.audio

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.rust.native.RustHostReports
import kotlinx.serialization.Serializable

internal sealed interface AudioHostReport {
    val path: String
    val revision: Long
    @Serializable
    data class Started(override val path: String, override val revision: Long) : AudioHostReport

    @Serializable
    data class Completed(override val path: String, override val revision: Long) : AudioHostReport

    @Serializable
    data class Progress(override val path: String, val positionMs: Long, override val revision: Long) : AudioHostReport

    suspend fun sendToRust() {
        when (this) {
            is Started -> RustHostReports.audioStarted(JsonHelper.jsonEncode(this))
            is Completed -> RustHostReports.audioCompleted(JsonHelper.jsonEncode(this))
            is Progress -> RustHostReports.audioProgress(JsonHelper.jsonEncode(this))
        }
    }
}
