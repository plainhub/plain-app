package com.ismartcoding.plain.platform

import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.ExportFileResultEvent
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent

/**
 * Singleton entry point that Swift calls after an iOS file pick / export
 * operation completes. Exposed to Swift via the PlainShared framework header.
 *
 * Swift's `FilePickerController` presents `PHPickerViewController` /
 * `UIDocumentPickerViewController`. When the user selects files (or cancels),
 * Swift calls back here:
 *  - [onPickResult] sends [PickFileResultEvent] so commonMain consumers
 *    (`ChatPage`, `FeedsPageEffects`, `BackupRestorePage`, …) can process the
 *    picked URIs. [uris] are `file://` URLs for the picked items.
 *  - [onExportResult] sends [ExportFileResultEvent] with the destination URI.
 *
 * Cancellation is logged but emits no event (commonMain consumers only react
 * to a real result).
 */
object IosFilePickerCallback {

    fun onPickResult(tag: String, type: String, uris: List<String>) {
        LogCat.d("IosFilePickerCallback: pickResult tag=$tag type=$type count=${uris.size}")
        if (uris.isEmpty()) return
        val pickTag = runCatching { PickFileTag.valueOf(tag) }.getOrNull() ?: run {
            LogCat.w("IosFilePickerCallback: unknown PickFileTag $tag")
            return
        }
        val pickType = runCatching { PickFileType.valueOf(type) }.getOrNull() ?: run {
            LogCat.w("IosFilePickerCallback: unknown PickFileType $type")
            return
        }
        sendEvent(PickFileResultEvent(pickTag, pickType, uris.toSet()))
    }

    fun onExportResult(type: String, uri: String) {
        LogCat.d("IosFilePickerCallback: exportResult type=$type uri=$uri")
        val exportType = runCatching { ExportFileType.valueOf(type) }.getOrNull() ?: run {
            LogCat.w("IosFilePickerCallback: unknown ExportFileType $type")
            return
        }
        sendEvent(ExportFileResultEvent(exportType, uri))
    }
}
