package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.chat.RustChatStore
import com.ismartcoding.plain.chat.ShareSendHelper
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.enums.ChatStatus
import com.ismartcoding.plain.features.share.ShareManager
import com.ismartcoding.plain.helpers.AppFileStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ChatServiceRustHttpTest {
    @Test
    fun pageShareForwardAndFolderUseRootCommandsWithActualOsSources() = runBlocking {
        val marker = "synthetic-chat-service-${UUID.randomUUID()}"
        val local = ChatTarget.parseId("peer:local")
        val root = File(com.ismartcoding.plain.appContext.cacheDir, marker).apply { mkdirs() }
        val source = File(root, "$marker.txt").apply { writeText("$marker actual source") }
        val ids = mutableSetOf<String>()
        val files = mutableSetOf<String>()
        val shares = mutableSetOf<String>()
        val oldService = com.ismartcoding.plain.preferences.UserPrefs.service.value
        suspend fun created(): List<DChat> = RustChatStore.getByPeerId("local").filter { it.content.toJSONString().contains(marker) }.also { rows -> ids += rows.map { it.id } }
        try {
            com.ismartcoding.plain.preferences.UserPrefs.service.set(true)
            val longText = marker + "😀".repeat(1100)
            val text = ChatManager.sendText(local, longText).also { ids += it.id }
            assertEquals(ChatStatus.SENT, text.status)
            val textFile = (text.content.value as DMessageFiles).items.single()
            files += textFile.uri.removePrefix("fid:").substringBefore('.')
            assertEquals(longText.toByteArray().size.toLong(), textFile.size)
            val record = AppFileStore.getById(files.single())!!
            assertArrayEquals(longText.toByteArray(), File(RustContentApi.directory, record.realPath).readBytes())
            assertTrue(ShareSendHelper.sendAsync(listOf(local, local), listOf(androidx.core.content.FileProvider.getUriForFile(com.ismartcoding.plain.appContext, com.ismartcoding.plain.AppIntents.AUTHORITY, source).toString()), null, "$marker caption"))
            val picked = created().filter { (it.content.value as? DMessageFiles)?.items?.any { item -> item.fileName == source.name } == true }
            assertEquals(2, picked.size)
            val attachment = (picked.first().content.value as DMessageFiles).items.single()
            val hash = attachment.uri.removePrefix("fid:").substringBefore('.')
            files += hash
            assertEquals(source.length(), attachment.size)
            assertEquals(2, AppFileStore.getById(hash)!!.refCount)
            val forward = ChatManager.forward(picked.first().id, local).also { ids += it.id }
            assertEquals(ChatStatus.SENT, forward.status)
            assertEquals(3, AppFileStore.getById(hash)!!.refCount)
            val encoded = com.ismartcoding.plain.helpers.UrlHelper.encrypt(attachment.uri)
            val request = BleBinaryFixture.file("local", encoded, 0, 8192)
            val rawReply = com.ismartcoding.plain.ble.server.HttpServiceHandler().handleRequest(request, "synthetic BLE MAC")
            val (status, body) = BleBinaryFixture.response(rawReply)
            assertEquals(200, status)
            assertArrayEquals(source.readBytes(), body)
            ChatManager.deleteOne(picked.first().id)
            assertEquals(2, AppFileStore.getById(hash)!!.refCount)
            val page = RustContentApi.query("""chatItems(target: "peer:local", offset: 0, limit: 20, query: "text:$marker") { id }""")["data"]!!.jsonObject["chatItems"]!!.jsonArray
            assertTrue("the sent message must appear in the contract page, got $page", page.any { it.jsonObject["id"]!!.jsonPrimitive.content == forward.id })
            val image = File(root, "$marker.png")
            val bitmap = android.graphics.Bitmap.createBitmap(7, 5, android.graphics.Bitmap.Config.ARGB_8888)
            image.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val imageUri = androidx.core.content.FileProvider.getUriForFile(com.ismartcoding.plain.appContext, com.ismartcoding.plain.AppIntents.AUTHORITY, image).toString()
            assertTrue(ShareSendHelper.sendAsync(listOf(local), listOf(imageUri), null, null))
            val picture = created().mapNotNull { it.content.value as? DMessageImages }.single().items.single()
            files += picture.uri.removePrefix("fid:").substringBefore('.')
            assertEquals(7, picture.width)
            assertEquals(5, picture.height)
            assertEquals(image.length(), picture.size)
            assertTrue(ShareSendHelper.sendFolderShareAsync(listOf(local), root.absolutePath, marker, null))
            val card = created().mapNotNull { it.content.value as? DMessageShare }.single()
            shares += card.shareId
            assertEquals(2, card.itemCount)
            assertEquals(source.length() + image.length(), card.totalSize)
            assertTrue(ShareManager.getShare(card.shareId)!!.readOnly)
            assertEquals(ShareManager.sharedToken(card.shareId), card.urlToken)
            val staging = File(RustContentApi.directory, "chat-picks")
            assertTrue(staging.listFiles().orEmpty().isEmpty())
        } finally {
            created()
            ChatManager.deleteByIds(ids)
            shares.forEach { ShareManager.deleteShare(it) }
            files.forEach { assertNull(AppFileStore.getById(it)) }
            root.deleteRecursively()
            com.ismartcoding.plain.preferences.UserPrefs.service.set(oldService)
            com.ismartcoding.plain.chat.ChatCacher.load()
        }
    }
}
