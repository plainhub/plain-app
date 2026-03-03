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

/**
 * Single source of truth for all [DChatChannel] data.
 *
 * Owns the channel list and all mutations. Any code that reads or writes channel
 * data should go through this ViewModel, eliminating the fragmentation that
 * previously existed between [PeerViewModel] and [ChatViewModel].
 *
 * Reacts to [ChannelUpdatedEvent] (fired by [ChannelSystemMessageHandler] on
 * remote updates and by this class after every local mutation) to keep its
 * [channels] state always current.
 */
class ChannelViewModel : ViewModel() {

    private val _channels = MutableStateFlow<List<DChatChannel>>(emptyList())

    /** Live, sorted list of all local channels. Observe this in the UI. */
    val channels: StateFlow<List<DChatChannel>> = _channels.asStateFlow()

    // ── UI dialog state ────────────────────────────────────────────

    val showCreateChannelDialog = mutableStateOf(false)
    val manageMembersChannelId = mutableStateOf<String?>(null)

    // ── Init ───────────────────────────────────────────────────────

    init {
        // Eagerly load channels from DB so the list is available immediately on first
        // composition, before any ChannelUpdatedEvent has been fired.
        refresh()

        // React to remote channel updates (e.g. from ChannelSystemMessageHandler)
        viewModelScope.launch {
            Channel.sharedFlow.collect { event ->
                if (event is ChannelUpdatedEvent) {
                    refresh()
                }
            }
        }
    }

    // ── Queries ────────────────────────────────────────────────────

    /** Reload channels from the database and update [channels]. */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val all = AppDatabase.instance.chatChannelDao().getAll()
                .sortedBy { it.name.lowercase() }
            _channels.value = all
        }
    }

    /**
     * Return the current snapshot of a single channel by [id], or null if not
     * found. Reads from the in-memory [channels] list — no DB round-trip.
     */
    fun getChannel(id: String?): DChatChannel? =
        id?.let { _channels.value.find { ch -> ch.id == it } }

    // ── Mutations ──────────────────────────────────────────────────

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
            // Notify all observers (including ChatViewModel) of the new channel
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

    fun addChannelMember(channelId: String, peerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val channel = AppDatabase.instance.chatChannelDao().getById(channelId) ?: return@launch
            if (channel.owner != "me") return@launch
            if (channel.hasMember(peerId)) return@launch

            val peer = AppDatabase.instance.peerDao().getById(peerId)
            channel.members = channel.members + ChannelMember(
                id = peerId,
                status = ChannelMember.STATUS_PENDING,
            )
            channel.version++
            channel.updatedAt = TimeHelper.now()
            AppDatabase.instance.chatChannelDao().update(channel)

            if (peer != null) {
                ChannelSystemMessageSender.sendInvite(channel, peer)
            }
            sendEvent(ChannelUpdatedEvent())
        }
    }

    fun resendInvite(channelId: String, peerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val channel = AppDatabase.instance.chatChannelDao().getById(channelId) ?: return@launch
            if (channel.owner != "me") return@launch
            val member = channel.findMember(peerId) ?: return@launch
            if (!member.isPending()) return@launch
            val peer = AppDatabase.instance.peerDao().getById(peerId) ?: return@launch
            ChannelSystemMessageSender.sendInvite(channel, peer)
        }
    }

    fun removeChannelMember(channelId: String, peerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val channel = AppDatabase.instance.chatChannelDao().getById(channelId) ?: return@launch
            if (channel.owner != "me") return@launch
            if (!channel.hasMember(peerId)) return@launch

            channel.members = channel.members.filter { it.id != peerId }
            channel.version++
            channel.updatedAt = TimeHelper.now()
            AppDatabase.instance.chatChannelDao().update(channel)

            val peer = AppDatabase.instance.peerDao().getById(peerId)
            if (peer != null) {
                ChannelSystemMessageSender.sendKick(channelId, peer, channel.key)
            }
            ChannelSystemMessageSender.broadcastUpdate(channel)
            sendEvent(ChannelUpdatedEvent())
        }
    }

    /** Non-owner member voluntarily leaves a channel. */
    fun leaveChannel(context: Context, channelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val channel = AppDatabase.instance.chatChannelDao().getById(channelId) ?: return@launch
            if (channel.owner == "me") return@launch

            val ownerPeer = AppDatabase.instance.peerDao().getById(channel.owner)
            if (ownerPeer != null) {
                ChannelSystemMessageSender.sendLeave(channelId, ownerPeer, channel.key)
            }
            channel.status = DChatChannel.STATUS_LEFT
            // Remove self from the members list so we no longer appear in the members grid
            channel.members = channel.members.filter { it.id != TempData.clientId }
            AppDatabase.instance.chatChannelDao().update(channel)
            ChatCacheManager.loadKeyCacheAsync()
            sendEvent(ChannelUpdatedEvent())
        }
    }

    /** Accept a received channel invite. */
    fun acceptChannelInvite(channelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val channel = AppDatabase.instance.chatChannelDao().getById(channelId) ?: return@launch
            val ownerPeer = AppDatabase.instance.peerDao().getById(channel.owner) ?: return@launch
            ChannelSystemMessageSender.sendInviteAccept(channelId, ownerPeer)
        }
    }

    /** Decline a received channel invite — remove it locally and notify the owner. */
    fun declineChannelInvite(context: Context, channelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val channel = AppDatabase.instance.chatChannelDao().getById(channelId) ?: return@launch
            val ownerPeer = AppDatabase.instance.peerDao().getById(channel.owner)
            if (ownerPeer != null) {
                ChannelSystemMessageSender.sendInviteDecline(channelId, ownerPeer)
            }
            ChatDbHelper.deleteAllChatsAsync(context, channelId)
            AppDatabase.instance.chatChannelDao().delete(channelId)
            ChatCacheManager.loadKeyCacheAsync()
            sendEvent(ChannelUpdatedEvent())
        }
    }
}
