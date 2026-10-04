package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.chat.ChatCacher
import com.ismartcoding.plain.chat.RustChatStore
import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.enums.ChatStatus
import com.ismartcoding.plain.features.ChatMessageEditor
import com.ismartcoding.plain.helpers.AppFileStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LinkPreviewRustHttpTest {
    @Test
    fun editUsesPersistedMessageAndReleasesPreviewImage() = runBlocking {
        val id = "synthetic-preview-${UUID.randomUUID()}"
        val url = "https://example.com/$id"
        var hash: String? = null
        try {
            val image = AppFileStore.importBytes(id.toByteArray(), "image/png")
            hash = image.id
            val chat = DChat(id = id, fromId = "local", toId = "local", status = ChatStatus.SENT,
                content = DMessageContent(MessageType.TEXT, DMessageText(url,
                    listOf(DLinkPreview(url = url, title = id, imageLocalPath = AppFileStore.toFidUri(image))))))
            RustChatStore.insert(chat)
            val stale = DChat(id = id, toId = "stale", content = DMessageContent(MessageType.TEXT, DMessageText("stale")))
            assertTrue(ChatMessageEditor.updateTextAsync(stale, "edited $id"))
            assertEquals("local", stale.toId)
            assertEquals(ChatStatus.SENT, stale.status)
            assertEquals("edited $id", (stale.content.value as DMessageText).text)
            assertTrue((stale.content.value as DMessageText).linkPreviews.isEmpty())
            assertNull(AppFileStore.getById(image.id))
            assertFalse(ChatMessageEditor.updateTextAsync(stale, "edited $id"))
            assertEquals(stale.content.toJSONString(), RustChatStore.getById(id)!!.content.toJSONString())
        } finally {
            RustChatStore.delete(id)
            hash?.let { while (AppFileStore.getById(it) != null) AppFileStore.release(it) }
            ChatCacher.load()
        }
    }
}
