package com.ismartcoding.plain.chat

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.toJSONString
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.serialization.json.*

internal object RustChatService {
    suspend fun send(target: ChatTarget, content: DMessageContent): DChat =
        RustChatStore.decode(call(ChatServiceCommand.Send(target.encodedToId, content.toJSONString())))
    suspend fun sendText(targets: List<ChatTarget>, text: String): List<DChat> =
        call(ChatServiceCommand.SendText(targets.map { it.encodedToId }, text)).jsonArray.map(RustChatStore::decode)
    suspend fun sendMany(targets: List<ChatTarget>, content: DMessageContent): List<DChat> =
        call(ChatServiceCommand.SendMany(targets.map { it.encodedToId }, content.toJSONString())).jsonArray.map(RustChatStore::decode)
    suspend fun forward(id: String, target: ChatTarget): DChat =
        RustChatStore.decode(call(ChatServiceCommand.Forward(id, target.encodedToId)))
    suspend fun share(targets: List<ChatTarget>, uris: List<String>, text: String?, caption: String?, images: Boolean? = null, normalize: Boolean = false): Boolean {
        val result = JsonHelper.jsonDecodeFromElement<ChatShareResult>(call(
            ChatServiceCommand.Share(targets.map { it.encodedToId }, uris, text, caption, images, normalize),
        ))
        result.warnings.forEach { DialogHelper.showMessage(it) }
        return result.ok
    }
    suspend fun folder(targets: List<ChatTarget>, path: String, name: String, expiry: String?): Boolean =
        JsonHelper.jsonDecodeFromElement<ChatShareResult>(call(
            ChatServiceCommand.Folder(targets.map { it.encodedToId }, path, name, expiry),
        )).ok
    suspend fun shareContent(id: String): DMessageContent = DChat.parseContent(call(ChatServiceCommand.ShareContent(id)).toString())
    suspend fun retry(id: String): DChat = RustChatStore.decode(call(ChatServiceCommand.Retry(id)))
    suspend fun delete(ids: Set<String>) { call(ChatServiceCommand.Delete(ids)) }
    suspend fun deleteQuery(query: String): Int = JsonHelper.jsonDecodeFromElement(call(ChatServiceCommand.DeleteQuery(query)))
    suspend fun createFiles(target: ChatTarget, files: List<DMessageFile>, images: Boolean): DChat =
        RustChatStore.decode(call(ChatServiceCommand.CreateFiles(target.encodedToId, files, images)))
    suspend fun replaceFiles(id: String, files: List<DMessageFile>): DChat? =
        call(ChatServiceCommand.ReplaceFiles(id, files)).takeUnless { it is JsonNull }?.let(RustChatStore::decode)
    suspend fun clear(target: ChatTarget) { call(ChatServiceCommand.Clear(target.encodedToId)) }
    suspend fun deliver(id: String, recipients: List<String>?): DChat {
        val result = RustContentApi.postJsonOrThrow("chat/send",
            JsonHelper.jsonEncodeToElement(ChatDeliveryRequest(id, recipients)).jsonObject, longRunning = true,
        ).getValue("result").jsonObject
        return result.getValue("chat").takeUnless { it is JsonNull }?.let(RustChatStore::decode)
            ?: error("Chat unavailable")
    }
    private suspend fun call(command: ChatServiceCommand): JsonElement =
        RustContentApi.postJsonOrThrow("chat/service", JsonHelper.jsonEncodeToElement(command).jsonObject,
            longRunning = true).getValue("result")
}
