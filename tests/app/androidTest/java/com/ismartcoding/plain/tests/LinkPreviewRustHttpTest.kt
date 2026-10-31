package com.ismartcoding.plain.tests

import java.util.UUID
import kotlinx.serialization.json.JsonObject
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.features.chat.RustChatStore
import com.ismartcoding.plain.features.chat.models.LinkPreview
import com.ismartcoding.plain.features.chat.models.MessageContent
import com.ismartcoding.plain.features.chat.models.MessageText
import com.ismartcoding.plain.features.chat.models.MessageType
import com.ismartcoding.plain.features.chat.models.toJSONString
import com.ismartcoding.plain.features.chat.ChatMessageEditor
import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.features.files.stored.AppFileStore
import kotlinx.serialization.json.jsonArray
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import com.ismartcoding.plain.features.chat.ChatClient
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import kotlinx.serialization.json.JsonPrimitive

@RunWith(AndroidJUnit4::class)
class LinkPreviewRustHttpTest {
    @Test
    fun editUsesPersistedMessageAndReleasesPreviewImage() = runBlocking {
        var id = "synthetic-preview-${UUID.randomUUID()}"
        val url = "https://example.com/$id"
        var hash: String? = null
        try {
            val image = AppFileFixtures.importBytes(id.toByteArray(), "image/png")
            hash = image.id
            val content = MessageContent(MessageType.TEXT, MessageText(url,
                listOf(LinkPreview(url = url, title = id, imageLocalPath = AppFileStore.toFidUri(image))))).toJSONString()
            val value = GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation(\$content:String!){sendChatItem(target: \"peer:local\", content: \$content) { id fromId toId channelId content status statusData createdAt updatedAt }}", mapOf("content" to JsonPrimitive(content)))).getValue("sendChatItem").jsonArray.single()
            val chat = JsonHelper.jsonDecodeFromElement<ChatItem>(value)
            id = chat.id
            val stale = ChatItem(id = id, toId = "stale", content = MessageContent(MessageType.TEXT, MessageText("stale")).toJSONString())
            assertTrue(ChatMessageEditor.updateTextAsync(stale, "edited $id"))
            assertEquals("local", stale.toId)
            assertEquals(chat.status, stale.status)
            assertEquals("edited $id", (stale.messageContent.value as MessageText).text)
            assertTrue((stale.messageContent.value as MessageText).linkPreviews.isEmpty())
            assertFalse(AppFileFixtures.exists(image.id))
            assertFalse(ChatMessageEditor.updateTextAsync(stale, "edited $id"))
            assertEquals(stale.content, RustChatStore.getById(id)!!.content)
        } finally {
            ChatClient.deleteOne(id)
            hash?.let { if (AppFileFixtures.exists(it)) AppFileStore.release(it) }
        }
    }
}
