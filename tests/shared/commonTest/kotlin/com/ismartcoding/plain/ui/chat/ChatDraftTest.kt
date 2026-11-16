package com.ismartcoding.plain.ui.chat

import com.ismartcoding.plain.features.chat.ChatDraftAttachment
import com.ismartcoding.plain.features.chat.chatDraftContent
import com.ismartcoding.plain.features.chat.models.ChatAttachmentType
import com.ismartcoding.plain.features.chat.models.ChatMessage
import com.ismartcoding.plain.features.chat.models.ChatTextFormat
import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.ui.chat.components.isChatMarkdownLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatDraftTest {
    @Test
    fun preservesMixedPickerOrderInOneMessage() {
        val picked = linkedMapOf(
            "first" to ChatDraftAttachment(ChatAttachmentType.FILE, "one.txt"),
            "second" to ChatDraftAttachment(ChatAttachmentType.IMAGE, "two.png"),
            "last" to ChatDraftAttachment(ChatAttachmentType.FILE, "three.txt"),
        )
        val prepared = picked.entries.map { (key, file) ->
            key to MessageFile(id = key, uri = "fid:$key", size = 1, fileName = file.name)
        }
        val message = chatDraftContent("**Hello**", ChatTextFormat.MARKDOWN, picked, prepared)?.value as ChatMessage
        assertEquals("**Hello**", message.text)
        assertEquals(ChatTextFormat.MARKDOWN, message.format)
        assertEquals(listOf("first", "second", "last"), message.items.map { it.attachment.id })
        assertEquals(listOf(ChatAttachmentType.FILE, ChatAttachmentType.IMAGE, ChatAttachmentType.FILE), message.items.map { it.type })
        assertNull(chatDraftContent("**Hello**", ChatTextFormat.MARKDOWN, picked, prepared.dropLast(1)))
        assertNull(chatDraftContent(" ", ChatTextFormat.PLAIN, emptyMap(), emptyList()))
    }

    @Test
    fun chatMarkdownOpensOnlyWebOrEmailLinks() {
        assertTrue(isChatMarkdownLink("https://example.org/docs"))
        assertTrue(isChatMarkdownLink("HTTP://example.org"))
        assertTrue(isChatMarkdownLink("mailto:demo@localhost"))
        assertFalse(isChatMarkdownLink("file:///example.txt"))
        assertFalse(isChatMarkdownLink("//example.org"))
        assertFalse(isChatMarkdownLink("https:///example"))
        assertFalse(isChatMarkdownLink("http:not-an-address"))
    }
}
