package com.ismartcoding.plain

import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.i18n.LocaleHelper

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.ismartcoding.plain.platform.isQPlus
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.features.files.ExportFileType
import com.ismartcoding.plain.features.files.PickFileType
import com.ismartcoding.plain.events.ExportFileEvent
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.platform.system.FilePickHelper
import com.ismartcoding.plain.platform.isRPlus
import com.ismartcoding.plain.platform.files.backupToFile

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
            // Below Android 11 the photo picker is a Google Play Services backport whose
            // availability probe is unreliable: an outdated GMS still claims the module,
            // then hijacks the launch into a "update Google Play services" screen instead
            // of throwing, so no fallback can trigger. Only trust the system picker
            // (Android 13+) or the ACTION_PICK_IMAGES intent probe (Android 11-12);
            // older devices go straight to the document picker.
            val usePhotoPicker = isRPlus() &&
                ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(this)
            if (!usePhotoPicker) {
                LogCat.e("Photo picker not available, falling back to file picker")
                doPickFile(event)
                return
            }
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
    if (event.type == ExportFileType.BACKUP && Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
        backupToFile(event.fileName)
        return
    }
    try {
        exportFileType = event.type
        exportFileActivityLauncher.launch(
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = when (event.type) {
                    ExportFileType.BACKUP -> "application/zip"
                    ExportFileType.OPML -> "text/*"
                    ExportFileType.IMAGE_PNG -> "image/png"
                    ExportFileType.IMAGE_JPEG -> "image/jpeg"
                    ExportFileType.IMAGE_WEBP -> "image/webp"
                }
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, event.fileName)
            },
        )
    } catch (e: ActivityNotFoundException) {
        LogCat.e("No document creation app available")
        DialogHelper.showMessage(Res.string.file_picker_not_available)
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
        DialogHelper.showErrorMessage(LocaleHelper.getString(Res.string.file_picker_not_available))
    } catch (e: IllegalStateException) {
        LogCat.e("Error launching pick file activity: ${e.message}")
    }
}
