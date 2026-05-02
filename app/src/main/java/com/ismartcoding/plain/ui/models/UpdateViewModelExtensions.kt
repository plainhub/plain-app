package com.ismartcoding.plain.ui.models

import com.ismartcoding.lib.channel.ChannelEvent
import com.ismartcoding.plain.events.UpdateDownloadCompleteEvent
import com.ismartcoding.plain.events.UpdateDownloadFailedEvent
import com.ismartcoding.plain.events.UpdateDownloadProgressEvent

fun UpdateViewModel.consumeUpdateDownloadEvent(event: ChannelEvent): Boolean {
    return when (event) {
        is UpdateDownloadProgressEvent -> {
            onDownloadProgress(event.progress)
            true
        }

        is UpdateDownloadCompleteEvent -> {
            onDownloadComplete(event.filePath)
            true
        }

        is UpdateDownloadFailedEvent -> {
            onDownloadFailed()
            true
        }

        else -> false
    }
}