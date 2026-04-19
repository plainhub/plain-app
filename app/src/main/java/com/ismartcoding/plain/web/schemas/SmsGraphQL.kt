package com.ismartcoding.plain.web.schemas

import android.webkit.MimeTypeMap
import com.apurebase.kgraphql.GraphQLError
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import com.apurebase.kgraphql.schema.execution.Executor
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.DPendingMms
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.events.StartMmsPollingEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.sms.DMessageAttachment
import com.ismartcoding.plain.features.sms.MmsHelper
import com.ismartcoding.plain.features.sms.SmsConversationHelper
import com.ismartcoding.plain.features.sms.SmsHelper
import com.ismartcoding.plain.db.AppDatabase
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
        resolver { offset: Int, limit: Int, query: String ->
            Permission.READ_SMS.checkAsync(MainApp.instance)
            SmsHelper.searchAsync(MainApp.instance, query, limit, offset).map { it.toModel() }
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
        resolver { offset: Int, limit: Int, query: String ->
            Permission.READ_SMS.checkAsync(MainApp.instance)
            SmsConversationHelper.searchConversationsAsync(MainApp.instance, query, limit, offset).map { it.toModel() }
        }
    }
    query("smsCount") {
        resolver { query: String ->
            if (Permission.READ_SMS.enabledAndCanAsync(MainApp.instance)) {
                SmsHelper.countAsync(MainApp.instance, query)
            } else {
                0
            }
        }
    }
    query("smsConversationCount") {
        resolver { query: String ->
            if (Permission.READ_SMS.enabledAndCanAsync(MainApp.instance)) {
                SmsConversationHelper.conversationCountAsync(MainApp.instance, query)
            } else {
                0
            }
        }
    }
    query("archivedConversations") {
        resolver { ->
            Permission.READ_SMS.checkAsync(MainApp.instance)
            SmsConversationHelper.getArchivedConversations(MainApp.instance).map { it.toModel() }
        }
    }
    query("smsAllCounts") {
        resolver { ->
            if (Permission.READ_SMS.enabledAndCanAsync(MainApp.instance)) {
                SmsHelper.countAllAsync(MainApp.instance)
            } else {
                SmsHelper.SmsCounts(0, 0, 0, 0)
            }
        }
    }
    mutation("archiveConversation") {
        resolver { id: String, date: Long ->
            AppDatabase.instance.archivedConversationDao().insert(DArchivedConversation(conversationId = id, conversationDate = date))
            true
        }
    }
    mutation("unarchiveConversation") {
        resolver { id: String ->
            AppDatabase.instance.archivedConversationDao().delete(id)
            true
        }
    }
    mutation("sendSms") {
        resolver { number: String, body: String ->
            Permission.SEND_SMS.checkAsync(MainApp.instance)
            try {
                SmsHelper.sendText(number, body)
            } catch (e: Exception) {
                e.printStackTrace()
                throw GraphQLError(e.message ?: "Invalid SMS input")
            }
            true
        }
    }
    mutation("sendMms") {
        resolver { number: String, body: String, attachmentPaths: List<String>, threadId: String ->
            try {
                val context = MainApp.instance
                val resolvedAttachments = attachmentPaths.map { path ->
                    val resolvedPath = AppFileStore.resolveUri(context, path)
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
                TempData.pendingMmsMessages.add(pendingEntry)
                sendEvent(StartMmsPollingEvent(pendingId, launchTimeSec, resolvedAttachments.map { it.first }))
                pendingId
            } catch (e: Exception) {
                e.printStackTrace()
                throw GraphQLError(e.message ?: "Failed to launch SMS app for MMS")
            }
        }
    }
}
