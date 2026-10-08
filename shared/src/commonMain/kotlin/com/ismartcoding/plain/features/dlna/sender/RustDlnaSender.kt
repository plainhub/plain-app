package com.ismartcoding.plain.features.dlna.sender

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

internal object RustDlnaSender {
    private val projectionLock = com.ismartcoding.plain.platform.PlatformLock()
    private var version = -1L
    suspend fun call(command: CastCommand) {
        val result = RustContentApi.postJsonOrThrow("system/dlna-sender", JsonHelper.jsonEncodeToElement<CastCommand>(command).jsonObject)
        apply(result.getValue("result").jsonObject)
    }
    suspend fun refresh() = call(CastCommand.Snapshot)
    fun apply(row: JsonObject) {
        val state = JsonHelper.jsonDecodeFromElement<CastSnapshot>(row)
        projectionLock.withLock {
            if (state.version < version) return@withLock
            version = state.version
            DlnaDeviceScanner.apply(state.devices)
            CastPlayer.apply(state)
        }
    }
}
