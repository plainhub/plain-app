package com.ismartcoding.plain.features.chat.download

import com.ismartcoding.plain.features.downloads.DownloadStatus
import com.ismartcoding.plain.models.ChatDownloadSource
import com.ismartcoding.plain.models.Download
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatDownloadsTest {
    private fun task(id: String, messageId: String) = Download(
        id, "attachment.bin", ChatDownloadSource(messageId, "peer", listOf("same-file")), "/files", null,
        DownloadStatus.DOWNLOADING, 1, 3, 1, 1, 0, "attachment.bin", "", emptyList(),
    )

    @Test
    fun forwarded_attachments_keep_their_own_message_and_task_identity() {
        val first = task("first-task", "first-message")
        val second = task("second-task", "second-message")
        val tasks = mapOf(first.id to first, second.id to second)
        assertEquals("first-task", tasks.attachmentDownload("first-message", "same-file")?.id)
        assertEquals("second-task", tasks.attachmentDownload("second-message", "same-file")?.id)
        assertNull(tasks.attachmentDownload("other-message", "same-file"))
        assertNull(tasks.attachmentDownload("first-message", "other-file"))
    }

    @Test
    fun completed_records_prevent_recreating_a_task_before_message_updates_arrive() {
        val complete = task("task", "message").copy(status = DownloadStatus.COMPLETED)
        assertEquals(complete, mapOf(complete.id to complete).attachmentDownload("message", "same-file"))
    }
}
