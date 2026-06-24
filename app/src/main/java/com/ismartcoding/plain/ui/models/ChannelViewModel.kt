package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.extensions.toSortName
import com.ismartcoding.plain.chat.ChannelManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.events.ChannelUpdatedEvent
import com.ismartcoding.plain.ui.base.ToastManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChannelViewModel : ViewModel() {

    private val _channels = MutableStateFlow<List<DChatChannel>>(emptyList())
    val channels: StateFlow<List<DChatChannel>> = _channels.asStateFlow()

    val invitingIds = mutableStateSetOf<String>()
    val kickingIds = mutableStateSetOf<String>()

    val showCreateChannelDialog = mutableStateOf(false)
    val renameChannelId = mutableStateOf("")

    init {
        loadAll()

        viewModelScope.launch {
            Channel.sharedFlow.collect { event ->
                if (event is ChannelUpdatedEvent) {
                    loadAll()
                }
            }
        }
    }

    fun loadAll() {
        launchSafe {
            _channels.value = AppDatabase.instance.chatChannelDao().getAll()
                .sortedBy { it.name.toSortName() }
        }
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
            ChannelManager.declineInvite(context, channelId)
        }
    }
}
