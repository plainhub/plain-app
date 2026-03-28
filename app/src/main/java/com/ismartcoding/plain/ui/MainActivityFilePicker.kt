package com.ismartcoding.plain.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.ExportFileEvent
import com.ismartcoding.plain.events.ExportFileResultEvent
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.FilePickHelper

internal fun MainActivity.handlePickFileEvent(event: PickFileEvent) {
    try {
        pickFileType = event.type
        pickFileTag = event.tag
        var type: ActivityResultContracts.PickVisualMedia.VisualMediaType? = null
        when (event.type) {
            PickFileType.IMAGE_VIDEO -> type = ActivityResultContracts.PickVisualMedia.ImageAndVideo
            PickFileType.IMAGE -> type = ActivityResultContracts.PickVisualMedia.ImageOnly
            else -> {}
        }
        if (type != null) {
            try {
                if (event.multiple) pickMultipleMedia.launch(PickVisualMediaRequest(type))
                else pickMedia.launch(PickVisualMediaRequest(type))
            } catch (e: ActivityNotFoundException) {
                LogCat.e("Photo picker not available, falling back to file picker")
                doPickFile(event)
            }
        } else {
            doPickFile(event)
        }
    } catch (e: IllegalStateException) {
        LogCat.e("Error launching pick file: ${e.message}")
    }
}

internal fun MainActivity.handleExportFileEvent(event: ExportFileEvent) {
    try {
        exportFileType = event.type
        exportFileActivityLauncher.launch(
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = when (event.type) {
                    ExportFileType.BACKUP -> "application/zip"
                    else -> "text/*"
                }
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, event.fileName)
            },
        )
    } catch (e: ActivityNotFoundException) {
        LogCat.e("No document creation app available")
        DialogHelper.showMessage(getString(R.string.file_picker_not_available))
    } catch (e: IllegalStateException) {
        LogCat.e("Error launching export file: ${e.message}")
    }
}

internal fun MainActivity.doPickFile(event: PickFileEvent) {
    try {
        val intent = when (event.type) {
            PickFileType.FOLDER -> FilePickHelper.getPickFolderIntent()
            else if (isQPlus()) -> FilePickHelper.getPickFileIntent(event.multiple)
            else -> FilePickHelper.getFallbackPickFileIntent(event.multiple)
        }
        pickFileActivityLauncher.launch(intent)
    } catch (e: ActivityNotFoundException) {
        LogCat.e("No file picker available on this device")
        DialogHelper.showErrorMessage(getString(R.string.file_picker_not_available))
    } catch (e: IllegalStateException) {
        LogCat.e("Error launching pick file activity: ${e.message}")
    }
}
