package com.ismartcoding.plain.tests

import java.util.UUID
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.GraphqlApi

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.features.chat.ChatClient
import com.ismartcoding.plain.features.chat.RustChatStore
import com.ismartcoding.plain.features.chat.ShareSendClient
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.features.chat.models.ChatMessage
import com.ismartcoding.plain.features.chat.models.MessageShare
import com.ismartcoding.plain.features.chat.ChatStatus
import com.ismartcoding.plain.features.shares.SharesClient
import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.features.files.stored.AppFileStore
import com.ismartcoding.plain.rust.RustRuntime
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.ismartcoding.plain.app.AppIntents
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.chat.sendPickedAttachments
import com.ismartcoding.plain.platform.network.ble.server.HttpServiceHandler
import com.ismartcoding.plain.Prefs

@RunWith(AndroidJUnit4::class)
class ChatServiceRustHttpTest {
    @Test
    fun pageShareForwardAndFolderUseNativeGraphqlWithActualOsSources() = runBlocking {
        val marker = "synthetic-chat-service-${UUID.randomUUID()}"
        val local = ChatTarget.parseId("peer:local")
        val root = File(appContext.cacheDir, marker).apply { mkdirs() }
        val source = File(root, "$marker.txt").apply { writeText("$marker actual source") }
        val ids = mutableSetOf<String>()
        val files = mutableSetOf<String>()
        val shares = mutableSetOf<String>()
        val oldService = Prefs.backgroundServiceEnabled.value
        suspend fun created(): List<ChatItem> = RustChatStore.getByPeerId("local").filter { com.ismartcoding.plain.lib.JsonHelper.jsonEncode(it.content).contains(marker) }.also { rows -> ids += rows.map { it.id } }
        try {
            Prefs.backgroundServiceEnabled.value = true
            val longText = marker + "😀".repeat(1100)
            val text = ChatClient.sendText(local, longText).also { ids += it.id }
            assertEquals(ChatStatus.SENT, text.status)
            val longMessage = text.messageContent.value as ChatMessage
            assertEquals(longText, longMessage.text)
            assertTrue(longMessage.items.isEmpty())
            assertTrue(sendPickedAttachments(listOf(local, local), listOf(FileProvider.getUriForFile(appContext, AppIntents.AUTHORITY, source).toString()), null, "$marker caption"))
            val picked = created().filter { (it.messageContent.value as? ChatMessage)?.files?.any { item -> item.fileName == source.name } == true }
            assertEquals(1, picked.size)
            assertEquals("$marker caption", (picked.single().messageContent.value as ChatMessage).text)
            val attachment = (picked.first().messageContent.value as ChatMessage).files.single()
            val hash = attachment.uri.removePrefix("fid:").substringBefore('.')
            files += hash
            assertEquals(source.length(), attachment.size)
            val forward = ChatClient.forward(picked.first().id, listOf(local)).single().also { ids += it.id }
            assertEquals(ChatStatus.SENT, forward.status)
            val encoded = ResourceCrypto.encrypt(attachment.uri)
            val request = BleBinaryFixture.file("local", encoded, 0, 8192)
            val rawReply = HttpServiceHandler().handleRequest(request, "synthetic BLE MAC")
            val (status, body) = BleBinaryFixture.response(rawReply)
            assertEquals(200, status)
            assertArrayEquals(source.readBytes(), body)
            ChatClient.deleteOne(picked.first().id)
            val page = GraphqlApi.execute(GraphqlRequest<JsonObject>("query { " + """chatItems(target: "peer:local", offset: 0, limit: 20, query: "text:$marker") { id }""" + " }"))["chatItems"]!!.jsonArray
            assertTrue("the sent message must appear in the contract page, got $page", page.any { it.jsonObject["id"]!!.jsonPrimitive.content == forward.id })
            val image = File(root, "$marker.png")
            val bitmap = Bitmap.createBitmap(7, 5, Bitmap.Config.ARGB_8888)
            image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val imageUri = FileProvider.getUriForFile(appContext, AppIntents.AUTHORITY, image).toString()
            assertTrue(sendPickedAttachments(listOf(local), listOf(imageUri), null, null))
            val picture = created().flatMap { (it.messageContent.value as? ChatMessage)?.images.orEmpty() }.single()
            files += picture.uri.removePrefix("fid:").substringBefore('.')
            assertEquals(7, picture.width)
            assertEquals(5, picture.height)
            assertEquals(image.length(), picture.size)
            assertEquals(1, ShareSendClient.sendFolderShare(listOf(local), root.absolutePath, marker, null).size)
            val card = created().mapNotNull { it.messageContent.value as? MessageShare }.single()
            shares += card.shareId
            assertEquals(2, card.itemCount)
            assertEquals(source.length() + image.length(), card.totalSize)
            assertTrue(SharesClient.getShare(card.shareId)!!.readOnly)
            assertEquals(android.net.Uri.parse(SharesClient.getShare(card.shareId)!!.url).fragment, card.urlToken)
            val staging = File(RustRuntime.dataDir, "chat-picks")
            assertTrue(staging.listFiles().orEmpty().isEmpty())
        } finally {
            created()
            ChatClient.deleteByIds(ids)
            shares.forEach { SharesClient.deleteShare(it) }
            files.forEach { assertFalse(AppFileFixtures.exists(it)) }
            root.deleteRecursively()
            Prefs.backgroundServiceEnabled.value = oldService
        }
    }
}
