package com.ismartcoding.plain.ui.casting

import com.ismartcoding.plain.ui.state.launchSafe

import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.withLock
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.IMedia
import com.ismartcoding.plain.features.dlna.sender.*
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.graphql.Subscription

class CastViewModel internal constructor(internal val source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.rust.graphql.ContentSource.Local) : ViewModel() {
    private val sender = source.cast
    internal val devices get() = sender.devices
    internal val items get() = sender.items
    internal val currentUri get() = sender.currentUri
    internal val isPlaying get() = sender.isPlaying
    internal val progressMs get() = sender.progressMs
    internal val durationMs get() = sender.durationMs
    internal val supportsCallback get() = sender.supportsCallback
    private val commands = kotlinx.coroutines.sync.Mutex()
    init {
        if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) {
            viewModelScope.launchSafe {
                val request = Subscription.dlnaSenderChanged()
                source.client.subscribe(request.operation).collect { response ->
                    check(response.errors.isEmpty()) { response.errors.joinToString { it.message } }
                    sender.apply(request.decode(checkNotNull(response.data)))
                }
            }
        }
    }
    fun startScan() = command(CastCommand.StartScan)
    fun stopScan() { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launchSafe { commands.withLock { sender.call(CastCommand.StopScan) } } }
    val castMode = mutableStateOf(false)
    val showCastDialog = mutableStateOf(false)
    val isLoading = mutableStateOf(false)
    val currentDeviceName: String get() = sender.currentDevice?.name.orEmpty()
    val hasCurrentDevice: Boolean get() = sender.currentDevice != null

    fun enterCastMode() { castMode.value = true; showCastDialog.value = false }
    fun selectDevice(hostAddress: String) {
        val device = sender.devices.value.firstOrNull { it.hostAddress == hostAddress } ?: return
        command(CastCommand.Select(device.id))
    }
    fun exitCastMode() { castMode.value = false; command(CastCommand.Exit) }
    fun stopCast() { castMode.value = false; command(CastCommand.Stop) }
    fun playCast() = command(CastCommand.Play)
    fun pauseCast() = command(CastCommand.Pause)
    fun seekCast(positionMs: Float) = command(CastCommand.Seek(positionMs.toLong()))
    fun cast(path: String) = castPath(path)
    fun cast(item: IMedia) = castItem(item)
    fun castPath(path: String) = castItemCommand(CastItem(path, path.getFilenameWithoutExtensionFromPath()))
    fun castItem(item: IMedia) { castItemCommand(CastItem.from(item)) }
    fun reorderCastItems(from: Int, to: Int) = command(CastCommand.Reorder(from, to))
    fun addCastItem(item: IMedia) = command(CastCommand.Add(CastItem.from(item)))
    fun removeCastItem(item: IMedia) = command(CastCommand.Remove(item.path))
    fun clearCastItems() = command(CastCommand.Clear)
    fun removeCastItemAt(index: Int) = command(CastCommand.RemoveAt(index))
    private fun command(command: CastCommand) {
        viewModelScope.launchSafe { commands.withLock { sender.call(command) } }
    }
    private fun castItemCommand(item: CastItem) {
        viewModelScope.launchSafe {
            isLoading.value = true
            try {
                commands.withLock { sender.call(CastCommand.Cast(item)) }
            }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { DialogHelper.showErrorMessage(error.message ?: "Cast failed") }
            finally { isLoading.value = false }
        }
    }
}
