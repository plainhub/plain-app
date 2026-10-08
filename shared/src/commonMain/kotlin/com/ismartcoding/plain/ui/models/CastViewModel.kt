package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.db.IMedia
import com.ismartcoding.plain.features.dlna.sender.*
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.plain.ui.helpers.DialogHelper

class CastViewModel : ViewModel() {
    val castMode = mutableStateOf(false)
    val showCastDialog = mutableStateOf(false)
    val isLoading = mutableStateOf(false)
    val currentDeviceName: String get() = CastPlayer.currentDevice?.name.orEmpty()
    val hasCurrentDevice: Boolean get() = CastPlayer.currentDevice != null

    fun enterCastMode() { castMode.value = true; showCastDialog.value = false }
    fun selectDevice(hostAddress: String) {
        val device = DlnaDeviceScanner.devices.value.firstOrNull { it.hostAddress == hostAddress } ?: return
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
    fun clearCastItems() = command(CastCommand.Clear)
    fun removeCastItemAt(index: Int) = command(CastCommand.RemoveAt(index))
    private fun command(command: CastCommand) { viewModelScope.launchSafe { RustDlnaSender.call(command) } }
    private fun castItemCommand(item: CastItem) {
        viewModelScope.launchSafe {
            isLoading.value = true
            try { RustDlnaSender.call(CastCommand.Cast(item)) }
            catch (error: Exception) { DialogHelper.showErrorMessage(error.message ?: "Cast failed") }
            finally { isLoading.value = false }
        }
    }
}
