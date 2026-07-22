package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.channel.ChannelManager
import com.ismartcoding.plain.ui.base.ToastManager

class ChannelViewModel : ViewModel() {

    val pendingIds = mutableStateSetOf<String>()

    val showCreateChannelDialog = mutableStateOf(false)
    val renameChannelId = mutableStateOf("")

    fun load() {
        viewModelScope.launchSafe { ChannelCacher.load() }
    }

    fun createChannel(name: String) {
        viewModelScope.launchSafe {
            ChannelManager.createChannel(name)
            showCreateChannelDialog.value = false
        }
    }

    fun renameChannel(channelId: String, newName: String) {
        viewModelScope.launchSafe {
            ChannelManager.renameChannel(channelId, newName)
            renameChannelId.value = ""
        }
    }

    fun removeChannel(channelId: String) {
        viewModelScope.launchSafe {
            ChannelManager.deleteChannel(channelId)
        }
    }

    fun leaveChannel(channelId: String) {
        viewModelScope.launchSafe {
            ChannelManager.leaveChannel(channelId)
        }
    }

    fun inviteMember(channelId: String, peerId: String) {
        viewModelScope.launchSafe(onDone = {
            pendingIds.remove(peerId)
        }) {
            pendingIds.add(peerId)
            ChannelManager.inviteMember(channelId, peerId)
        }
    }

    fun resendInvite(channelId: String, peerId: String) {
        viewModelScope.launchSafe {
            ChannelManager.resendInvite(channelId, peerId)
        }
    }

    fun kickMember(channelId: String, peerId: String) {
        viewModelScope.launchSafe(onDone = {
            pendingIds.remove(peerId)
        }) {
            pendingIds.add(peerId)
            ChannelManager.kickMember(channelId, peerId)
        }
    }

    fun acceptInvite(
        channelId: String,
        onSuccess: () -> Unit = {},
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launchSafe(onDone = onDone) {
            val r = ChannelManager.acceptInvite(channelId)
            if (r.isSuccess) {
                onSuccess()
            } else {
                ToastManager.showErrorToast(r.getError())
            }
        }
    }

    fun declineInvite(
        channelId: String,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launchSafe(onDone = onDone) {
            ChannelManager.declineInvite(channelId)
        }
    }
}
