package com.ismartcoding.plain.features.dlna.sender

import com.ismartcoding.plain.IMedia
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.system.PlatformLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject

internal open class DlnaSenderClient(private val api: com.ismartcoding.plain.rust.graphql.GraphqlClient) {
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
        val request = Mutation.dlna(command)
        apply(api.execute(request))
    }

    fun apply(row: JsonObject) = apply(JsonHelper.jsonDecodeFromElement<CastSnapshot>(row))

    internal fun apply(state: CastSnapshot) {
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
