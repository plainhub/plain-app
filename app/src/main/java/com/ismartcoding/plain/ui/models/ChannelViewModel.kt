package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.extensions.toSortName
import com.ismartcoding.plain.chat.ChannelManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.events.ChannelUpdatedEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChannelViewModel : ViewModel() {

    private val _channels = MutableStateFlow<List<DChatChannel>>(emptyList())
    val channels: StateFlow<List<DChatChannel>> = _channels.asStateFlow()

    private val _loadingIds = MutableStateFlow<Set<String>>(emptySet())
    val loadingIds: StateFlow<Set<String>> = _loadingIds.asStateFlow()

    val showCreateChannelDialog = mutableStateOf(false)
    val renameChannelId = mutableStateOf("")
    val renameChannelName = mutableStateOf("")

    init {
        refresh()

        viewModelScope.launch {
            Channel.sharedFlow.collect { event ->
                if (event is ChannelUpdatedEvent) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        launchIO {
            _channels.value = AppDatabase.instance.chatChannelDao().getAll()
                .sortedBy { it.name.toSortName() }
        }
    }

    fun createChannel(name: String) {
        launchIO {
            ChannelManager.createChannel(name)
            showCreateChannelDialog.value = false
        }
    }

    fun renameChannel(channelId: String, newName: String) {
        launchIO {
            ChannelManager.renameChannel(channelId, newName)
            renameChannelId.value = ""
        }
    }

    fun removeChannel(context: Context, channelId: String) {
        launchIO {
            ChannelManager.deleteChannel(context, channelId)
        }
    }

    fun addChannelMember(channelId: String, peerId: String) {
        launchIO {
            ChannelManager.addMember(channelId, peerId)
        }
    }

    fun resendInvite(channelId: String, peerId: String) {
        launchIO {
            ChannelManager.resendInvite(channelId, peerId)
        }
    }

    fun removeChannelMember(channelId: String, peerId: String) {
        launchIO {
            ChannelManager.removeMember(channelId, peerId)
        }
    }

    fun leaveChannel(channelId: String) {
        launchIO {
            ChannelManager.leaveChannel(channelId)
        }
    }

    fun acceptChannelInvite(channelId: String) {
        launchIO {
            ChannelManager.acceptInvite(channelId)
        }
    }

    fun declineChannelInvite(context: Context, channelId: String) {
        launchIO {
            ChannelManager.declineInvite(context, channelId)
        }
    }
}
