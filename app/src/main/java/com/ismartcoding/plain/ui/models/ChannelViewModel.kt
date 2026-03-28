package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.ChannelSystemMessageSender
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.ChannelMember
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.events.ChannelUpdatedEvent
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelViewModel : ViewModel() {

    private val _channels = MutableStateFlow<List<DChatChannel>>(emptyList())
    val channels: StateFlow<List<DChatChannel>> = _channels.asStateFlow()

    val showCreateChannelDialog = mutableStateOf(false)
    val manageMembersChannelId = mutableStateOf<String?>(null)

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
        viewModelScope.launch(Dispatchers.IO) {
            val all = AppDatabase.instance.chatChannelDao().getAll()
                .sortedBy { it.name.lowercase() }
            _channels.value = all
        }
    }

    fun getChannel(id: String?): DChatChannel? =
        id?.let { _channels.value.find { ch -> ch.id == it } }

    fun createChannel(name: String, onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val channel = DChatChannel()
            channel.name = name.trim()
            channel.owner = "me"
            channel.key = CryptoHelper.generateChaCha20Key()
            channel.version = 1
            channel.members = listOf(ChannelMember(id = TempData.clientId))

            AppDatabase.instance.chatChannelDao().insert(channel)
            ChatCacheManager.loadKeyCacheAsync()
            sendEvent(ChannelUpdatedEvent())
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun renameChannel(channelId: String, newName: String, onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val channel = AppDatabase.instance.chatChannelDao().getById(channelId) ?: return@launch
            channel.name = newName.trim()
            channel.version++
            channel.updatedAt = TimeHelper.now()
            AppDatabase.instance.chatChannelDao().update(channel)
            if (channel.owner == "me") {
                ChannelSystemMessageSender.broadcastUpdate(channel)
            }
            sendEvent(ChannelUpdatedEvent())
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun removeChannel(context: Context, channelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val channel = AppDatabase.instance.chatChannelDao().getById(channelId) ?: return@launch
                if (channel.owner == "me") {
                    ChannelSystemMessageSender.broadcastKick(channel)
                }
                ChatDbHelper.deleteAllChatsAsync(context, channelId)
                AppDatabase.instance.chatChannelDao().delete(channelId)
                ChatCacheManager.loadKeyCacheAsync()
                sendEvent(ChannelUpdatedEvent())
            } catch (_: Exception) {
            }
        }
    }
}
