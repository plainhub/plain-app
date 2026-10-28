package com.ismartcoding.plain.ui.chat

import com.ismartcoding.plain.features.chat.ChatStatus
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.features.chat.models.MessageContent
import com.ismartcoding.plain.features.chat.models.MessageText
import com.ismartcoding.plain.features.chat.models.MessageType
import com.ismartcoding.plain.features.chat.models.toJSONString
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
    private val content = MessageContent(MessageType.TEXT, MessageText("hello"))

    @BeforeTest
    fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }

    @AfterTest
    fun cleanup() { ChatViewModel._itemsFlow.value = emptyList(); Dispatchers.resetMain() }

    private fun sent(target: ChatTarget) = ChatItem(
        id = "committed", fromId = "me", toId = target.toId,
        content = content.toJSONString(), status = ChatStatus.SENT,
    )

    @Test
    fun pendingMessageAppearsBeforeSendReturnsAndIsReplacedWithoutEventDuplicates() = runTest {
        ChatViewModel.initializeTargetAsync("peer:fixture")
        val target = ChatViewModel.target.value
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val sending = async {
            ChatViewModel.sendMessage(target, content) { to, _ ->
                started.complete(Unit)
                release.await()
                sent(to)
            }
        }
        started.await()
        assertEquals(ChatStatus.PENDING, ChatViewModel.itemsFlow.value.single().status)
        assertEquals("hello", (ChatViewModel.itemsFlow.value.single().value as MessageText).text)
        ChatViewModel.onMessagesCreated(target, listOf(sent(target)))
        release.complete(Unit)
        assertTrue(sending.await())
        assertEquals(listOf("committed"), ChatViewModel.itemsFlow.value.map { it.id })
        assertEquals(ChatStatus.SENT, ChatViewModel.itemsFlow.value.single().status)
        ChatViewModel.onMessagesCreated(target, listOf(sent(target)))
        assertEquals(1, ChatViewModel.itemsFlow.value.size)
    }

    @Test
    fun failedSendKeepsMessageAndRetryReplacesIt() = runTest {
        ChatViewModel.initializeTargetAsync("peer:fixture")
        val target = ChatViewModel.target.value
        assertFalse(ChatViewModel.sendMessage(target, content) { _, _ -> error("Offline") })
        val failed = ChatViewModel.itemsFlow.value.single()
        assertEquals(ChatStatus.FAILED, failed.status)
        assertTrue(ChatViewModel.sendMessage(target, content, pendingId = failed.id) { to, _ -> sent(to) })
        assertEquals(listOf("committed"), ChatViewModel.itemsFlow.value.map { it.id })
    }

    @Test
    fun switchingConversationsWhileSendingDoesNotInsertTheResultIntoTheNewConversation() = runTest {
        ChatViewModel.initializeTargetAsync("peer:fixture")
        val target = ChatViewModel.target.value
        assertTrue(ChatViewModel.sendMessage(target, content) { to, _ ->
            ChatViewModel.initializeTargetAsync("peer:other")
            ChatViewModel._itemsFlow.value = emptyList()
            sent(to)
        })
        assertTrue(ChatViewModel.itemsFlow.value.isEmpty())
    }

    @Test
    fun deliveryFailureUsesTheCommittedMessageForRetry() = runTest {
        ChatViewModel.initializeTargetAsync("peer:fixture")
        assertFalse(ChatViewModel.sendMessage(ChatViewModel.target.value, content) { to, _ ->
            sent(to).copy(status = ChatStatus.FAILED)
        })
        assertEquals("committed", ChatViewModel.itemsFlow.value.single().id)
        assertEquals(ChatStatus.FAILED, ChatViewModel.itemsFlow.value.single().status)
    }
}
