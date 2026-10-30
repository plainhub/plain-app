package com.ismartcoding.plain.ui.chat

import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.features.chat.ChatStatus
import com.ismartcoding.plain.features.chat.models.MessageContent
import com.ismartcoding.plain.features.chat.models.MessageText
import com.ismartcoding.plain.features.chat.models.MessageType
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRecoveryTest {
    @BeforeTest
    fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }

    private fun chat(id: String, text: String) = ChatItem(
        id = id, fromId = "peer", toId = "me", status = ChatStatus.SENT,
        content = JsonHelper.jsonEncode(MessageContent(MessageType.TEXT, MessageText(text))),
        createdAt = Instant.parse("2026-01-01T00:00:00Z"), updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
    private fun text() = (ChatViewModel.local.itemsFlow.value.single().value as MessageText).text

    @AfterTest
    fun cleanup() { ChatViewModel.local._itemsFlow.value = emptyList(); Dispatchers.resetMain() }

    @Test
    fun recoveryRefreshesOnlyLoadedMessagesAndAppliesCommittedUpdatesAndDeletions() = runTest {
        ChatViewModel.local.initializeTargetAsync("peer:fixture")
        ChatViewModel.local._itemsFlow.value = listOf(VChat.from(chat("loaded", "old")), VChat.from(chat("deleted", "old")))
        var calls = 0
        ChatViewModel.local.refreshLoaded { ids ->
            calls++
            assertEquals(listOf("loaded", "deleted"), ids)
            mapOf("loaded" to chat("loaded", "committed"), "deleted" to null)
        }
        assertEquals(1, calls)
        assertEquals("committed", text())
        ChatViewModel.local._itemsFlow.value = emptyList()
        ChatViewModel.local.refreshLoaded { error("Empty view must not fetch") }
    }

    @Test
    fun aNewerLiveUpdateIsPreservedWhenTheRecoveryReadWasAlreadyInFlight() = runTest {
        ChatViewModel.local.initializeTargetAsync("peer:fixture")
        ChatViewModel.local._itemsFlow.value = listOf(VChat.from(chat("loaded", "old")))
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val recovery = async {
            ChatViewModel.local.refreshLoaded { started.complete(Unit); release.await(); mapOf("loaded" to chat("loaded", "snapshot")) }
        }
        started.await()
        ChatViewModel.local.update(chat("loaded", "newer live event"))
        release.complete(Unit)
        recovery.await()
        assertEquals("newer live event", text())
    }

    @Test
    fun switchingConversationsDiscardsTheOldRecoverySnapshot() = runTest {
        ChatViewModel.local.initializeTargetAsync("peer:fixture")
        ChatViewModel.local._itemsFlow.value = listOf(VChat.from(chat("loaded", "old")))
        ChatViewModel.local.refreshLoaded {
            ChatViewModel.local.initializeTargetAsync("peer:other")
            ChatViewModel.local._itemsFlow.value = listOf(VChat.from(chat("other", "current conversation")))
            mapOf("loaded" to chat("loaded", "stale"))
        }
        assertEquals("other", ChatViewModel.local.itemsFlow.value.single().id)
        assertEquals("current conversation", text())
    }

    @Test
    fun failedRecoveryDoesNotReplaceCurrentMessageState() = runTest {
        ChatViewModel.local.initializeTargetAsync("peer:fixture")
        ChatViewModel.local._itemsFlow.value = listOf(VChat.from(chat("loaded", "old")))
        assertFailsWith<IllegalStateException> { ChatViewModel.local.refreshLoaded { error("read failed") } }
        assertEquals("old", text())
    }
}
