package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.sms.DMessage
import com.ismartcoding.plain.features.sms.DMessageAttachment
import com.ismartcoding.plain.features.sms.DPendingMms
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.events.HStartMmsPollingEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.platform.countMedia
import com.ismartcoding.plain.platform.countSmsConversations
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.platform.fileExists
import com.ismartcoding.plain.platform.getArchivedSmsConversations
import com.ismartcoding.plain.platform.getSmsAllCounts
import com.ismartcoding.plain.platform.launchDefaultSmsApp
import com.ismartcoding.plain.platform.mimeTypeFromExtension
import com.ismartcoding.plain.platform.resolveAppFileUri
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.platform.searchSmsConversations
import com.ismartcoding.plain.platform.sendSmsText
import com.ismartcoding.plain.db.DArchivedConversation
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.httpserver.loaders.TagsLoader
import com.ismartcoding.plain.httpserver.models.Message
import com.ismartcoding.plain.httpserver.models.MessageConversation
import com.ismartcoding.plain.httpserver.models.SmsCounts
import com.ismartcoding.plain.httpserver.models.toModel

@GraphQLQuery
suspend fun smsAllCounts(): SmsCounts {
    return if (Permission.READ_SMS.enabledAndIsGrantedAsync()) {
        getSmsAllCounts().toModel()
    } else {
        SmsCounts(0, 0, 0, 0)
    }
}

@GraphQLMutation
suspend fun unarchiveConversation(id: String): Boolean {
    AppDatabase.instance.archivedConversationDao().delete(id)
    return true
}

@GraphQLMutation
suspend fun sendSms(number: String, body: String, subscriptionId: Int): Boolean {
    Permission.SEND_SMS.checkEnabledAsync()
    val simId = if (subscriptionId >= 0) subscriptionId else null
    try {
        sendSmsText(number, body, simId)
    } catch (e: Exception) {
        e.printStackTrace()
        throw GraphQLError(e.message ?: "Invalid SMS input")
    }
    return true
}

@GraphQLQuery
suspend fun sms(offset: Int, limit: Int, query: String): List<Message> {
    Permission.READ_SMS.checkEnabledAsync()
    return searchMedia(DataType.SMS, query, limit, offset, FileSortBy.DATE_DESC)
        .filterIsInstance<DMessage>()
        .map { it.toModel() }
}

@GraphQLQuery
suspend fun smsConversations(offset: Int, limit: Int, query: String): List<MessageConversation> {
    Permission.READ_SMS.checkEnabledAsync()
    return searchSmsConversations(query, limit, offset).map { it.toModel() }
}

@GraphQLQuery
suspend fun smsCount(query: String): Int {
    return if (Permission.READ_SMS.enabledAndIsGrantedAsync()) {
        countMedia(DataType.SMS, query)
    } else {
        0
    }
}

@GraphQLQuery
suspend fun smsConversationCount(query: String): Int {
    return if (Permission.READ_SMS.enabledAndIsGrantedAsync()) {
        countSmsConversations(query)
    } else {
        0
    }
}

@GraphQLQuery
suspend fun archivedConversations(): List<MessageConversation> {
    Permission.READ_SMS.checkEnabledAsync()
    return getArchivedSmsConversations().map { it.toModel() }
}

@GraphQLMutation
suspend fun archiveConversation(id: String, date: Long): Boolean {
    AppDatabase.instance.archivedConversationDao().insert(DArchivedConversation(conversationId = id, conversationDate = date))
    return true
}

@GraphQLMutation
suspend fun sendMms(number: String, body: String, attachmentPaths: List<String>, threadId: String): String {
    try {
        val resolvedAttachments = attachmentPaths.map { path ->
            val resolvedPath = resolveAppFileUri(path)
            if (!fileExists(resolvedPath)) {
                throw IllegalArgumentException("Attachment file not found: $resolvedPath")
            }
            val mimeType = mimeTypeFromExtension(resolvedPath.getFilenameExtension())
            Pair(resolvedPath, mimeType)
        }
        val launchTimeSec = launchDefaultSmsApp(number, body, resolvedAttachments)
        val nowMs = TimeHelper.nowMillis()

        val pendingId = "pending_mms_$nowMs"
        val pendingEntry = DPendingMms(
            id = pendingId,
            number = number,
            body = body,
            attachments = resolvedAttachments.map { (path, mimeType) ->
                DMessageAttachment(path, mimeType, path.getFilenameFromPath())
            },
            threadId = threadId,
            launchTimeSec = launchTimeSec,
            createdAt = TimeHelper.now(),
        )
        TempData.pendingMmsMessages.add(pendingEntry)
        sendEvent(HStartMmsPollingEvent(pendingId, launchTimeSec, resolvedAttachments.map { it.first }))
        return pendingId
    } catch (e: Exception) {
        e.printStackTrace()
        throw GraphQLError(e.message ?: "Failed to launch SMS app for MMS")
    }
}

fun SchemaBuilder.addSmsSchema() {
    type<Message> {
        dataProperty("tags") {
            prepare { item -> item.id.value }
            loader { ids ->
                TagsLoader.load(ids, DataType.SMS)
            }
        }
    }
}
