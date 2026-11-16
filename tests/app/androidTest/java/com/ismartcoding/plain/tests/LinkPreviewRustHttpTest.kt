package com.ismartcoding.plain.tests

import java.util.UUID
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.features.chat.ChatClient
import com.ismartcoding.plain.features.chat.ChatMessageEditor
import com.ismartcoding.plain.features.chat.RustChatStore
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.features.chat.models.*
import com.ismartcoding.plain.features.files.stored.AppFileStore
import com.ismartcoding.plain.models.ChatContent
import com.ismartcoding.plain.models.ChatItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinkPreviewRustHttpTest {
    @Test
    fun editUsesPersistedMessageAndKeepsItsFormatAndAttachment() = runBlocking {
        val marker = "synthetic-chat-edit-${UUID.randomUUID()}"
        val file = AppFileFixtures.importBytes(marker.toByteArray(), "text/plain")
        var id: String? = null
        try {
            val attachment = ChatMessageAttachment(ChatAttachmentType.FILE, MessageFile(uri = AppFileStore.toFidUri(file), size = file.size, fileName = "$marker.txt"))
            val message = ChatMessage("**$marker**", ChatTextFormat.MARKDOWN, listOf(attachment))
            val sent = ChatClient.sendContent(ChatTarget.parseId("peer:local"), MessageContent(MessageType.MESSAGE, message))
            id = sent.id
            val stale = ChatItem(id = sent.id, toId = "stale", content = ChatContent.Message("stale", ChatTextFormat.PLAIN, emptyList(), emptyList()))
            assertTrue(ChatMessageEditor.updateTextAsync(stale, "edited $marker"))
            assertEquals("local", stale.toId)
            assertEquals(sent.status, stale.status)
            val edited = stale.messageContent.value as ChatMessage
            assertEquals("edited $marker", edited.text)
            assertEquals(ChatTextFormat.MARKDOWN, edited.format)
            assertEquals((sent.messageContent.value as ChatMessage).items, edited.items)
            assertTrue(edited.linkPreviews.isEmpty())
            assertFalse(ChatMessageEditor.updateTextAsync(stale, "edited $marker"))
            assertEquals(stale.content, RustChatStore.getById(sent.id)!!.content)
        } finally {
            id?.let { ChatClient.deleteOne(it) }
            if (AppFileFixtures.exists(file.id)) AppFileStore.release(file.id)
        }
    }
}
