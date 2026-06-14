package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.plain.chat.ChannelManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.events.ChannelUpdatedEvent
import com.ismartcoding.plain.ui.base.runHandling
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
            val ok = runHandling { ChannelManager.createChannel(name) }
            if (ok != null) withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun renameChannel(channelId: String, newName: String, onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            // renameChannel throws if the channel is missing — surface the
            // message but still call onDone so the dialog can dismiss.
            runHandling { ChannelManager.renameChannel(channelId, newName) }
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun removeChannel(context: Context, channelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runHandling { ChannelManager.deleteChannel(context, channelId) }
        }
    }
}
