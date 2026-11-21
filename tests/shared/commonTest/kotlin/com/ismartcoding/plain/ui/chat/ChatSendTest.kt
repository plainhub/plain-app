package com.ismartcoding.plain.ui.chat

import com.ismartcoding.plain.features.chat.ChatStatus
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.features.chat.models.MessageContent
import com.ismartcoding.plain.features.chat.models.ChatMessage
import com.ismartcoding.plain.features.chat.models.ChatTextFormat
import com.ismartcoding.plain.features.chat.models.MessageType
import com.ismartcoding.plain.models.ChatItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ChatSendTest {
    private val content = MessageContent(MessageType.MESSAGE, ChatMessage(text = "hello"))

    @BeforeTest
    fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }

    @AfterTest
    fun cleanup() { ChatViewModel.local._itemsFlow.value = emptyList(); Dispatchers.resetMain() }

    private fun sent(target: ChatTarget) = ChatItem(
        id = "committed", fromId = "me", toId = target.toId,
        content = com.ismartcoding.plain.models.ChatContent.Message("hello", ChatTextFormat.PLAIN, emptyList(), emptyList()), status = ChatStatus.SENT,
    )

    @Test
    fun committedPendingMessageIsTheOnlyRowWhileDeliveryIsInFlight() = runTest {
        ChatViewModel.local.initializeTargetAsync("peer:fixture")
        val target = ChatViewModel.local.target.value
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val sending = async {
            ChatViewModel.local.sendMessage(target, content) { to, _ ->
                started.complete(Unit)
                release.await()
                sent(to)
            }
        }
        started.await()
        val pending = sent(target).copy(status = ChatStatus.PENDING)
        ChatViewModel.local.onMessagesCreated(target, listOf(pending), scroll = true)
        val rowsDuringDelivery = ChatViewModel.local.itemsFlow.value
        release.complete(Unit)
        assertTrue(sending.await())
        assertEquals(listOf(pending.id to ChatStatus.PENDING), rowsDuringDelivery.map { it.id to it.status })
        assertEquals(listOf("committed"), ChatViewModel.local.itemsFlow.value.map { it.id })
        assertEquals(ChatStatus.SENT, ChatViewModel.local.itemsFlow.value.single().status)
        ChatViewModel.local.onMessagesCreated(target, listOf(pending), scroll = true)
        assertEquals(ChatStatus.SENT, ChatViewModel.local.itemsFlow.value.single().status)
    }

    @Test
    fun failureBeforeCommitReportsTheErrorWithoutInsertingAMessage() = runTest {
        ChatViewModel.local.initializeTargetAsync("peer:fixture")
        val target = ChatViewModel.local.target.value
        val failure = assertFailsWith<IllegalStateException> {
            ChatViewModel.local.sendMessage(target, content) { _, _ -> error("Offline") }
        }
        assertEquals("Offline", failure.message)
        assertTrue(ChatViewModel.local.itemsFlow.value.isEmpty())
        assertTrue(ChatViewModel.local.sendMessage(target, content) { to, _ -> sent(to) })
        assertEquals(listOf("committed"), ChatViewModel.local.itemsFlow.value.map { it.id })
    }

    @Test
    fun switchingConversationsWhileSendingDoesNotInsertTheResultIntoTheNewConversation() = runTest {
        ChatViewModel.local.initializeTargetAsync("peer:fixture")
        val target = ChatViewModel.local.target.value
        assertTrue(ChatViewModel.local.sendMessage(target, content) { to, _ ->
            ChatViewModel.local.initializeTargetAsync("peer:other")
            ChatViewModel.local._itemsFlow.value = emptyList()
            sent(to)
        })
        assertTrue(ChatViewModel.local.itemsFlow.value.isEmpty())
    }

    @Test
    fun deliveryFailureUsesTheCommittedMessageForRetry() = runTest {
        ChatViewModel.local.initializeTargetAsync("peer:fixture")
        assertFalse(ChatViewModel.local.sendMessage(ChatViewModel.local.target.value, content) { to, _ ->
            sent(to).copy(status = ChatStatus.FAILED)
        })
        assertEquals("committed", ChatViewModel.local.itemsFlow.value.single().id)
        assertEquals(ChatStatus.FAILED, ChatViewModel.local.itemsFlow.value.single().status)
    }
}
