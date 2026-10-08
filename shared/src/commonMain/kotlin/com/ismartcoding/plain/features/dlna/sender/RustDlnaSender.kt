package com.ismartcoding.plain.features.dlna.sender

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.db.IMedia
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.platform.PlatformLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * The one Kotlin mirror of the Rust cast runtime: it owns the projected state
 * and is the only caller of `POST /system/dlna-sender`. State arrives on
 * websocket event 10008, commands go out over the same endpoint.
 */
internal object RustDlnaSender {
    private val projectionLock = PlatformLock()
    private var version = -1L
    var currentDevice: DlnaDevice? = null
        private set
    private val deviceState = MutableStateFlow<List<DlnaDevice>>(emptyList())
    val devices = deviceState.asStateFlow()
    private val itemState = MutableStateFlow<List<IMedia>>(emptyList())
    val items = itemState.asStateFlow()
    private val uriState = MutableStateFlow("")
    val currentUri = uriState.asStateFlow()
    val isPlaying = MutableStateFlow(false)
    val progressMs = MutableStateFlow(0f)
    val durationMs = MutableStateFlow(0f)
    val supportsCallback = MutableStateFlow(false)

    suspend fun call(command: CastCommand) {
        val result = RustContentApi.postJsonOrThrow("system/dlna-sender", JsonHelper.jsonEncodeToElement<CastCommand>(command).jsonObject)
        apply(result.getValue("result").jsonObject)
    }
    suspend fun refresh() = call(CastCommand.Snapshot)
    fun addItem(item: IMedia) { coIO { call(CastCommand.Add(CastItem.from(item))) } }
    fun removeItem(item: IMedia) { coIO { call(CastCommand.Remove(item.path)) } }
    fun startScan() { coIO { call(CastCommand.StartScan) } }
    fun stopScan() { coIO { call(CastCommand.StopScan) } }

    fun apply(row: JsonObject) {
        val state = JsonHelper.jsonDecodeFromElement<CastSnapshot>(row)
        projectionLock.withLock {
            if (state.version < version) return@withLock
            version = state.version
            deviceState.value = state.devices
            currentDevice = state.currentDevice
            itemState.value = state.items
            uriState.value = state.currentUri
            isPlaying.value = state.playing
            progressMs.value = state.progressMs.toFloat()
            durationMs.value = state.durationMs.toFloat()
            supportsCallback.value = state.supportsCallback
        }
    }
}
