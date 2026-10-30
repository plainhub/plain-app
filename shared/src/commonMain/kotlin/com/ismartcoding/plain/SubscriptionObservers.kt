package com.ismartcoding.plain

import com.ismartcoding.plain.features.chat.ChatMessageReceiver
import com.ismartcoding.plain.features.chat.RustChatStore
import com.ismartcoding.plain.features.chat.channel.channelGraphql
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.features.chat.peer.RustPeerStore
import com.ismartcoding.plain.features.files.FavoriteFoldersClient
import com.ismartcoding.plain.ui.chat.ChatViewModel
import com.ismartcoding.plain.ui.notes.NotesViewModel
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.rust.graphql.AppSubscriptions
import com.ismartcoding.plain.rust.graphql.SubscriptionUpdate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

import com.ismartcoding.plain.features.chat.channel.ChannelSystemMessageReceiver
import com.ismartcoding.plain.features.chat.download.DownloadQueue
import com.ismartcoding.plain.features.images.search.RustImageModels
import com.ismartcoding.plain.features.nearby.PairingProjection
import com.ismartcoding.plain.features.nearby.RustMdnsRuntime
import com.ismartcoding.plain.features.nearby.RustNearbyDevices
import com.ismartcoding.plain.ui.pomodoro.PomodoroProjection

internal object SubscriptionObservers {
    fun start(scope: CoroutineScope) {
        scope.observe(AppSubscriptions.mdnsChanged) { RustMdnsRuntime.refresh() }
        scope.observe(AppSubscriptions.nearbyDevicesChanged) { RustNearbyDevices.apply(RustNearbyDevices.snapshot()) }
        scope.observe(AppSubscriptions.downloadsChanged) { DownloadQueue.refresh() }
        scope.observe(AppSubscriptions.pairingChanged) { PairingProjection.reconcile() }
        scope.observeData(AppSubscriptions.imageModelsUpdated) { value -> RustImageModels.apply(value) }
        scope.observeData(AppSubscriptions.channelsChanged) { value -> ChannelSystemMessageReceiver.applyCommitted(value) }
        scope.observeData(AppSubscriptions.pairingRequested) { value -> PairingProjection.request(value) }
        scope.observe(AppSubscriptions.pomodoroAction) { update ->
            when (update) {
                is SubscriptionUpdate.Data -> PomodoroProjection.apply(update.value)
                SubscriptionUpdate.Attached -> PomodoroProjection.refresh()
                is SubscriptionUpdate.Failed -> Unit
            }
        }
        scope.observe(AppSubscriptions.contentChanged) {
            try { FavoriteFoldersClient.refresh() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { LogCat.e("Favorite folders refresh", error) }
            NotesViewModel.local.reloadAsync()
        }
        scope.observeChatChanges(AppSubscriptions.messagesCreated) { values ->
            values.forEach { value ->
                RustChatStore.getById(value.id)?.let { item ->
                    if (item.fromId == "me") {
                        val target = if (item.channelId.isNullOrEmpty()) ChatTarget.parseId("peer:" + item.toId)
                            else ChatTarget.parseId("channel:" + item.channelId)
                        ChatViewModel.local.onMessagesCreated(target, listOf(item), scroll = true)
                    } else RustPeerStore.getById(item.fromId)?.let { peer ->
                        val channel = item.channelId?.takeIf { it.isNotEmpty() }?.let { channelGraphql(it) }
                        ChatMessageReceiver.applyCommitted(item, peer, channel)
                    }
                }
            }
        }
        scope.observeChatChanges(AppSubscriptions.messagesUpdated) { values ->
            values.mapNotNull { RustChatStore.getById(it.id) }.forEach { ChatViewModel.local.update(it) }
        }
        scope.observeChatChanges(AppSubscriptions.messagesDeleted) { target ->
            if (target.startsWith("ids=")) ChatViewModel.local.onMessagesDeleted(target.removePrefix("ids=").split(',').filter { it.isNotEmpty() }.toSet())
            else ChatViewModel.local.onConversationCleared(target)
        }
    }
    private fun <T> CoroutineScope.observe(source: Flow<SubscriptionUpdate<T>>, consume: suspend (SubscriptionUpdate<T>) -> Unit) = launch(start = CoroutineStart.UNDISPATCHED) {
        source.collect { update ->
            try {
                if (update is SubscriptionUpdate.Failed) throw update.error
                consume(update)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { LogCat.e("Apply GraphQL subscription", error) }
        }
    }
    private fun <T> CoroutineScope.observeData(source: Flow<SubscriptionUpdate<T>>, consume: suspend (T) -> Unit) = observe(source) { update ->
        if (update is SubscriptionUpdate.Data) consume(update.value)
    }
    private fun <T> CoroutineScope.observeChatChanges(source: Flow<SubscriptionUpdate<T>>, consume: suspend (T) -> Unit) = observe(source) { update ->
        when (update) {
            is SubscriptionUpdate.Data -> consume(update.value)
            SubscriptionUpdate.Attached -> if (ChatViewModel.local.visibleConversationId.value != null) ChatViewModel.local.fetchAsync(ChatViewModel.local.target.value.toId)
            is SubscriptionUpdate.Failed -> Unit
        }
    }
}
