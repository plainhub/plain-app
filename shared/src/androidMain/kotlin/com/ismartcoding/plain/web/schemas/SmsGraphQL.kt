package com.ismartcoding.plain.web.schemas
import com.ismartcoding.plain.appContext

import android.webkit.MimeTypeMap
import com.ismartcoding.plain.features.sms.DPendingMms
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.AndroidTempData
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.events.HStartMmsPollingEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.features.checkEnabledAsync
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.features.sms.DMessageAttachment
import com.ismartcoding.plain.features.sms.MmsHelper
import com.ismartcoding.plain.features.sms.SmsConversationHelper
import com.ismartcoding.plain.features.sms.SmsHelper
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DArchivedConversation
import com.ismartcoding.plain.helpers.AppFileStore
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.Message
import com.ismartcoding.plain.web.models.toModel
import java.io.File
import kotlin.time.Instant

fun SchemaBuilder.addSmsSchema() {
    query("sms") {
        configure {
            executor = Executor.DataLoaderPrepared
        }
        resolver("offset", "limit", "query") { offset: Int, limit: Int, query: String ->
            Permission.READ_SMS.checkEnabledAsync()
            SmsHelper.searchAsync(appContext, query, limit, offset).map { it.toModel() }
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
            SmsConversationHelper.searchConversationsAsync(appContext, query, limit, offset).map { it.toModel() }
        }
    }
    query("smsCount") {
        resolver("query") { query: String ->
            if (Permission.READ_SMS.enabledAndIsGrantedAsync()) {
                SmsHelper.countAsync(appContext, query)
            } else {
                0
            }
        }
    }
    query("smsConversationCount") {
        resolver("query") { query: String ->
            if (Permission.READ_SMS.enabledAndIsGrantedAsync()) {
                SmsConversationHelper.conversationCountAsync(appContext, query)
            } else {
                0
            }
        }
    }
    query("archivedConversations") {
        resolver { ->
            Permission.READ_SMS.checkEnabledAsync()
            SmsConversationHelper.getArchivedConversations(appContext).map { it.toModel() }
        }
    }
    query("smsAllCounts") {
        resolver { ->
            if (Permission.READ_SMS.enabledAndIsGrantedAsync()) {
                SmsHelper.countAllAsync(appContext)
            } else {
                SmsHelper.SmsCounts(0, 0, 0, 0)
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
                SmsHelper.sendText(number, body, simId)
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
                val context = appContext
                val resolvedAttachments = attachmentPaths.map { path ->
                    val resolvedPath = AppFileStore.resolveUri(path)
                    val file = File(resolvedPath)
                    if (!file.exists()) {
                        throw IllegalArgumentException("Attachment file not found: $resolvedPath")
                    }
                    val mimeType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(file.extension)
                        ?: "application/octet-stream"
                    Pair(resolvedPath, mimeType)
                }
                val launchTimeSec = System.currentTimeMillis() / 1000 - 1
                MmsHelper.launchDefaultSmsApp(number, body, resolvedAttachments)

                val pendingId = "pending_mms_${System.currentTimeMillis()}"
                val pendingEntry = DPendingMms(
                    id = pendingId,
                    number = number,
                    body = body,
                    attachments = resolvedAttachments.map { (path, mimeType) ->
                        DMessageAttachment(path, mimeType, File(path).name)
                    },
                    threadId = threadId,
                    launchTimeSec = launchTimeSec,
                    createdAt = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                )
                AndroidTempData.pendingMmsMessages.add(pendingEntry)
                sendEvent(HStartMmsPollingEvent(pendingId, launchTimeSec, resolvedAttachments.map { it.first }))
                pendingId
            } catch (e: Exception) {
                e.printStackTrace()
                throw GraphQLError(e.message ?: "Failed to launch SMS app for MMS")
            }
        }
    }
}
