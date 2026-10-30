package com.ismartcoding.plain.ui.chat

import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filter
import com.ismartcoding.plain.rust.graphql.AppSubscriptions
import com.ismartcoding.plain.rust.graphql.changes
import com.ismartcoding.plain.ui.state.launchSafe

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.models.ChatChannel
import com.ismartcoding.plain.ui.components.ToastManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChannelViewModel internal constructor(
    private val api: com.ismartcoding.plain.rust.graphql.GraphqlClient = com.ismartcoding.plain.rust.graphql.GraphqlApi,
    private val readChannels: suspend () -> List<ChatChannel> = { api.execute(com.ismartcoding.plain.graphql.Query.chatChannels()) },
    private val sourceChanges: kotlinx.coroutines.flow.Flow<Unit>? = null,
) : ViewModel() {
    private val lock = Mutex()
    val channels = MutableStateFlow<List<ChatChannel>>(emptyList())

    val pendingIds = mutableStateSetOf<String>()

    val showCreateChannelDialog = mutableStateOf(false)
    val renameChannelId = mutableStateOf("")

    suspend fun observe() = coroutineScope {
        launch(start = CoroutineStart.UNDISPATCHED) {
            (sourceChanges ?: merge(
                AppSubscriptions.contentChanged.changes(),
                AppSubscriptions.channelsChanged.changes(),
            )).collect { refreshSafely() }
        }
        refreshSafely()
    }

    private suspend fun refresh() = lock.withLock {
        channels.value = readChannels()
    }

    private suspend fun refreshSafely() {
        try { refresh() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { LogCat.e("Load channels", error) }
    }

    fun load() {
        viewModelScope.launch { refreshSafely() }
    }

    fun createChannel(name: String) {
        viewModelScope.launchSafe {
            api.execute(com.ismartcoding.plain.graphql.Mutation.createChatChannel(name))
            showCreateChannelDialog.value = false
        }
    }

    fun renameChannel(channelId: String, newName: String) {
        viewModelScope.launchSafe {
            api.execute(com.ismartcoding.plain.graphql.Mutation.updateChatChannel(channelId, newName))
            renameChannelId.value = ""
        }
    }

    fun removeChannel(channelId: String) {
        viewModelScope.launchSafe {
            api.execute(com.ismartcoding.plain.graphql.Mutation.deleteChatChannel(channelId))
        }
    }

    fun leaveChannel(channelId: String) {
        viewModelScope.launchSafe {
            api.execute(com.ismartcoding.plain.graphql.Mutation.leaveChatChannel(channelId))
        }
    }

    fun inviteMember(channelId: String, peerId: String) {
        viewModelScope.launchSafe(onDone = {
            pendingIds.remove(peerId)
        }) {
            pendingIds.add(peerId)
            api.execute(com.ismartcoding.plain.graphql.Mutation.addChatChannelMember(channelId, peerId))
        }
    }

    fun resendInvite(channelId: String, peerId: String) {
        viewModelScope.launchSafe {
            api.execute(com.ismartcoding.plain.graphql.Mutation.addChatChannelMember(channelId, peerId))
        }
    }

    fun kickMember(channelId: String, peerId: String) {
        viewModelScope.launchSafe(onDone = {
            pendingIds.remove(peerId)
        }) {
            pendingIds.add(peerId)
            api.execute(com.ismartcoding.plain.graphql.Mutation.removeChatChannelMember(channelId, peerId))
        }
    }

    fun acceptInvite(
        channelId: String,
        onSuccess: () -> Unit = {},
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launchSafe(onDone = onDone) {
            api.execute(com.ismartcoding.plain.graphql.Mutation.acceptChatChannelInvite(channelId))
            onSuccess()
        }
    }

    fun declineInvite(
        channelId: String,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launchSafe(onDone = onDone) {
            api.execute(com.ismartcoding.plain.graphql.Mutation.declineChatChannelInvite(channelId))
        }
    }
}
