package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.RustChatStore
import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.helpers.AppFileStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AttachmentImportRustHttpTest {
    private suspend fun call(body: JsonObject) = RustContentApi.postJson("chat/attachment", body).getValue("result")
    private suspend fun begin(chat: String, file: DMessageFile) = call(buildJsonObject {
        put("action", "begin"); put("message_id", chat); put("id", file.id); put("uri", file.uri)
    }).jsonObject
    private suspend fun finish(ticket: JsonObject) = call(buildJsonObject {
        put("action", "finish"); put("token", ticket.getValue("token"))
    }).jsonObject
    private suspend fun abort(ticket: JsonObject) = call(buildJsonObject {
        put("action", "abort"); put("token", ticket.getValue("token"))
    }).jsonPrimitive.boolean

    @Test
    fun rustCommitsConcurrentAttachmentsAndDeduplicatesReferences() = runBlocking {
        val id = "synthetic-attachment-${UUID.randomUUID()}"
        val bytes = id.toByteArray()
        val a = DMessageFile(id = "$id-a", uri = "fsid:$id-a", fileName = "../../a.txt", size = bytes.size.toLong())
        val b = a.copy(id = "$id-b", uri = "fsid:$id-b", fileName = "b.txt")
        val chat = DChat(id = id, content = DMessageContent(MessageType.FILES, DMessageFiles(listOf(a, b))))
        val tickets = mutableListOf<JsonObject>()
        var imported: String? = null
        try {
            RustChatStore.insert(chat)
            val first = begin(id, a).also(tickets::add)
            val second = begin(id, b).also(tickets::add)
            for (ticket in tickets) {
                val path = ticket.getValue("path").jsonPrimitive.content
                assertEquals(File(RustContentApi.directory, "attachment-transfers").canonicalPath, File(path).parentFile!!.canonicalPath)
                File(path).writeBytes(bytes)
            }
            val receipt = finish(first)
            imported = receipt.getValue("uri").jsonPrimitive.content.removePrefix("fid:").substringBefore('.')
            finish(second)
            val files = (RustChatStore.getById(id)!!.content.value as DMessageFiles).items
            assertTrue(files.all { it.uri == receipt.getValue("uri").jsonPrimitive.content })
            assertEquals(2, AppFileStore.getById(imported!!)!!.refCount)
            assertFalse(File(first.getValue("path").jsonPrimitive.content).exists())
            var rejected = false
            try { finish(first) } catch (_: IllegalStateException) { rejected = true }
            assertTrue(rejected)
        } finally {
            tickets.forEach { abort(it) }
            RustChatStore.delete(id)
            imported?.let { hash -> while (AppFileStore.getById(hash) != null) AppFileStore.release(hash) }
            com.ismartcoding.plain.chat.ChatCacher.load()
        }
    }

    @Test
    fun shortDeletedAndStaleTransfersNeverCommit() = runBlocking {
        val id = "synthetic-attachment-errors-${UUID.randomUUID()}"
        val bytes = id.toByteArray()
        val file = DMessageFile(id = "$id-file", uri = "fsid:$id-file", fileName = "fixture.txt", size = bytes.size.toLong())
        val chat = DChat(id = id, content = DMessageContent(MessageType.FILES, DMessageFiles(listOf(file))))
        val tickets = mutableListOf<JsonObject>()
        try {
            RustChatStore.insert(chat)
            val old = begin(id, file).also(tickets::add)
            assertTrue(abort(old))
            val current = begin(id, file).also(tickets::add)
            assertFalse(abort(old))
            File(current.getValue("path").jsonPrimitive.content).writeBytes(bytes.dropLast(1).toByteArray())
            var shortRejected = false
            try { finish(current) } catch (_: IllegalStateException) { shortRejected = true }
            assertTrue(shortRejected)
            assertEquals(file.uri, (RustChatStore.getById(id)!!.content.value as DMessageFiles).items.single().uri)
            val deleted = begin(id, file).also(tickets::add)
            File(deleted.getValue("path").jsonPrimitive.content).writeBytes(bytes)
            RustChatStore.delete(id)
            var deletedRejected = false
            try { finish(deleted) } catch (_: IllegalStateException) { deletedRejected = true }
            assertTrue(deletedRejected)
            assertFalse(File(deleted.getValue("path").jsonPrimitive.content).exists())
        } finally {
            tickets.forEach { abort(it) }
            RustChatStore.delete(id)
            com.ismartcoding.plain.chat.ChatCacher.load()
        }
    }
}
