package com.ismartcoding.plain.features.media

import com.ismartcoding.plain.db.IMedia
import com.ismartcoding.plain.features.dlna.sender.*
import com.ismartcoding.plain.lib.coIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object CastPlayer {
    var currentDevice: DlnaDevice? = null
        private set
    private val itemState = MutableStateFlow<List<IMedia>>(emptyList())
    val items = itemState.asStateFlow()
    private val uriState = MutableStateFlow("")
    val currentUri = uriState.asStateFlow()
    val isPlaying = MutableStateFlow(false)
    val progressMs = MutableStateFlow(0f)
    val durationMs = MutableStateFlow(0f)
    val supportsCallback = MutableStateFlow(false)
    val active = MutableStateFlow(false)

    fun addItem(item: IMedia) {
        coIO { RustDlnaSender.call(CastCommand.Add(CastItem.from(item))) }
    }
    fun removeItem(item: IMedia) { coIO { RustDlnaSender.call(CastCommand.Remove(item.path)) } }
    fun removeItemAt(index: Int) { coIO { RustDlnaSender.call(CastCommand.RemoveAt(index)) } }
    fun clearItems() { coIO { RustDlnaSender.call(CastCommand.Clear) } }
    fun reorderItems(fromIndex: Int, toIndex: Int) { coIO { RustDlnaSender.call(CastCommand.Reorder(fromIndex, toIndex)) } }
    internal fun apply(state: CastSnapshot) {
        currentDevice = state.currentDevice
        itemState.value = state.items
        uriState.value = state.currentUri
        isPlaying.value = state.playing
        progressMs.value = state.progressMs.toFloat()
        durationMs.value = state.durationMs.toFloat()
        supportsCallback.value = state.supportsCallback
        active.value = state.active
    }
}
