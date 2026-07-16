package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.sms.DMessage
import com.ismartcoding.plain.features.sms.DMessageAttachment
import com.ismartcoding.plain.features.sms.DPendingMms
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.events.HStartMmsPollingEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.platform.SmsCounts
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
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.Message
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addSmsSchema() {
    query("sms") {
        configure {
            executor = Executor.DataLoaderPrepared
        }
        resolver("offset", "limit", "query") { offset: Int, limit: Int, query: String ->
            Permission.READ_SMS.checkEnabledAsync()
            searchMedia(DataType.SMS, query, limit, offset, FileSortBy.DATE_DESC)
                .filterIsInstance<DMessage>()
                .map { it.toModel() }
        }
        type<Message> {
            dataProperty("tags") {
                prepare { item -> item.id.value }
                loader { ids ->
                    TagsLoader.load(ids, DataType.SMS)
                }
            }
        }
    }
    query("smsConversations") {
        resolver("offset", "limit", "query") { offset: Int, limit: Int, query: String ->
            Permission.READ_SMS.checkEnabledAsync()
            searchSmsConversations(query, limit, offset).map { it.toModel() }
        }
    }
    query("smsCount") {
        resolver("query") { query: String ->
            if (Permission.READ_SMS.enabledAndIsGrantedAsync()) {
                countMedia(DataType.SMS, query)
            } else {
                0
            }
        }
    }
    query("smsConversationCount") {
        resolver("query") { query: String ->
            if (Permission.READ_SMS.enabledAndIsGrantedAsync()) {
                countSmsConversations(query)
            } else {
                0
            }
        }
    }
    query("archivedConversations") {
        resolver { ->
            Permission.READ_SMS.checkEnabledAsync()
            getArchivedSmsConversations().map { it.toModel() }
        }
    }
    query("smsAllCounts") {
        resolver { ->
            if (Permission.READ_SMS.enabledAndIsGrantedAsync()) {
                getSmsAllCounts()
            } else {
                SmsCounts(0, 0, 0, 0)
            }
        }
    }
    mutation("archiveConversation") {
        resolver("id", "date") { id: String, date: Long ->
            AppDatabase.instance.archivedConversationDao().insert(DArchivedConversation(conversationId = id, conversationDate = date))
            true
        }
    }
    mutation("unarchiveConversation") {
        resolver("id") { id: String ->
            AppDatabase.instance.archivedConversationDao().delete(id)
            true
        }
    }
    mutation("sendSms") {
        resolver("number", "body", "subscriptionId") { number: String, body: String, subscriptionId: Int ->
            Permission.SEND_SMS.checkEnabledAsync()
            val simId = if (subscriptionId >= 0) subscriptionId else null
            try {
                sendSmsText(number, body, simId)
            } catch (e: Exception) {
                e.printStackTrace()
                throw GraphQLError(e.message ?: "Invalid SMS input")
            }
            true
        }
    }
    mutation("sendMms") {
        resolver("number", "body", "attachmentPaths", "threadId") { number: String, body: String, attachmentPaths: List<String>, threadId: String ->
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
                pendingId
            } catch (e: Exception) {
                e.printStackTrace()
                throw GraphQLError(e.message ?: "Failed to launch SMS app for MMS")
            }
        }
    }
}
