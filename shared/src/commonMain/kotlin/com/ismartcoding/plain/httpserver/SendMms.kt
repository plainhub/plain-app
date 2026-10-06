package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.events.HStartMmsPollingEvent
import com.ismartcoding.plain.features.sms.DMessageAttachment
import com.ismartcoding.plain.features.sms.DPendingMms
import com.ismartcoding.plain.features.sms.SmsProviderContract
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.fileExists
import com.ismartcoding.plain.platform.getLatestSentMmsId
import com.ismartcoding.plain.platform.launchDefaultSmsApp
import com.ismartcoding.plain.platform.mimeTypeFromExtension
import com.ismartcoding.plain.platform.resolveAppFileUri
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val mmsSendMutex = Mutex()

/**
 * Compose an MMS in the device's default SMS app and track the send. Returns
 * a pendingId consumed by the mms polling/WebSocket flow — poll with it until
 * the send resolves.
 *
 * Rust drives this through the `systemSendMms` host fact, so it is a plain
 * suspend function here: the public contract field is implemented in Rust and
 * calls back into it.
 */
@OptIn(ExperimentalUuidApi::class)
suspend fun sendMms(number: String, body: String, attachmentPaths: List<String>, threadId: ID): String = mmsSendMutex.withLock {
    try {
        require(number.isNotBlank()) { "MMS recipient is required" }
        val resolvedAttachments = attachmentPaths.map { path ->
            val resolvedPath = resolveAppFileUri(path)
            if (!fileExists(resolvedPath)) {
                throw IllegalArgumentException("Attachment file not found: $resolvedPath")
            }
            val mimeType = mimeTypeFromExtension(resolvedPath.getFilenameExtension())
            Pair(resolvedPath, mimeType)
        }
        val requestedFingerprint = SmsProviderContract.MmsSendFingerprint(
            address = number,
            body = body,
            threadId = threadId.value,
            attachmentContentTypes = resolvedAttachments.map { it.second },
        )
        val duplicatePendingSend = TempData.pendingMmsMessages.any { pending ->
            SmsProviderContract.mmsOperationsAreIndistinguishable(
                requestedFingerprint,
                SmsProviderContract.MmsSendFingerprint(
                    address = pending.number,
                    body = pending.body,
                    threadId = pending.threadId,
                    attachmentContentTypes = pending.attachments.map { it.contentType },
                ),
            )
        }
        if (duplicatePendingSend) {
            throw IllegalStateException("An indistinguishable MMS send is already pending")
        }
        val minimumMmsId = getLatestSentMmsId()
        val launchTimeSec = launchDefaultSmsApp(number, body, resolvedAttachments)
        val pendingId = "pending_mms_${Uuid.random()}"
        val pendingEntry = DPendingMms(
            id = pendingId,
            number = number,
            body = body,
            attachments = resolvedAttachments.map { (path, mimeType) ->
                DMessageAttachment(path, mimeType, path.getFilenameFromPath())
            },
            threadId = threadId.value,
            launchTimeSec = launchTimeSec,
            createdAt = TimeHelper.now(),
        )
        TempData.pendingMmsMessages.add(pendingEntry)
        sendEvent(
            HStartMmsPollingEvent(
                pendingId,
                launchTimeSec,
                minimumMmsId,
                number,
                body,
                threadId.value,
                resolvedAttachments.map { it.first },
                resolvedAttachments.map { it.second },
            ),
        )
        pendingId
    } catch (e: Exception) {
        e.printStackTrace()
        throw GraphQLError(e.message ?: "Failed to launch SMS app for MMS")
    }
}