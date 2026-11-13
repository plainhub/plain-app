package com.ismartcoding.plain.features.chat.download

import com.ismartcoding.plain.models.ChatDownloadSource
import com.ismartcoding.plain.models.Download

fun Map<String, Download>.attachmentDownload(messageId: String, fileId: String): Download? =
    values.firstOrNull { task ->
        val source = task.source
        source is ChatDownloadSource && source.messageId == messageId && source.fileIds.contains(fileId)
    }
