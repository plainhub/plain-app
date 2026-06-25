package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.channel.ChannelManager
import com.ismartcoding.plain.ui.base.ToastManager

class ChannelViewModel : ViewModel() {

    val invitingIds = mutableStateSetOf<String>()
    val kickingIds = mutableStateSetOf<String>()

    val showCreateChannelDialog = mutableStateOf(false)
    val renameChannelId = mutableStateOf("")

    fun load() {
        launchSafe { ChannelCacher.load() }
    }

    fun createChannel(name: String) {
        launchSafe {
            ChannelManager.createChannel(name)
            showCreateChannelDialog.value = false
        }
    }

    fun renameChannel(channelId: String, newName: String) {
        launchSafe {
            ChannelManager.renameChannel(channelId, newName)
            renameChannelId.value = ""
        }
    }

    fun removeChannel(context: Context, channelId: String) {
        launchSafe {
            ChannelManager.deleteChannel(context, channelId)
        }
    }

    fun leaveChannel(channelId: String) {
        launchSafe {
            ChannelManager.leaveChannel(channelId)
        }
    }

    fun inviteMember(channelId: String, peerId: String) {
        launchSafe {
            invitingIds.add(peerId)
            ChannelManager.inviteMember(channelId, peerId)
            invitingIds.remove(peerId)
        }
    }

    fun resendInvite(channelId: String, peerId: String) {
        launchSafe {
            ChannelManager.resendInvite(channelId, peerId)
        }
    }

    fun kickMember(channelId: String, peerId: String) {
        launchSafe {
            kickingIds.add(peerId)
            ChannelManager.kickMember(channelId, peerId)
            kickingIds.remove(peerId)
        }
    }

    fun acceptInvite(channelId: String, onSuccess: () -> Unit = {}) {
        launchSafe {
            val r = ChannelManager.acceptInvite(channelId)
            if (r.isSuccess) {
                onSuccess()
            } else {
                ToastManager.showErrorToast(r.getError())
            }
        }
    }

    fun declineInvite(context: Context, channelId: String) {
        launchSafe {
            ChannelManager.declineInvite(channelId)
        }
    }
}
