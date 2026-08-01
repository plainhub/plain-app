package com.ismartcoding.plain.platform

import com.ismartcoding.plain.events.ExportFileEvent
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.receiveEventHandler
import kotlinx.coroutines.Job

/**
 * Registers handlers for [PickFileEvent] and [ExportFileEvent] on iOS. When
 * commonMain UI (chat input, feed import, backup/restore, etc.) emits one of
 * these events, the handler delegates to Swift via [IosPlatformRegistry.filePicker].
 *
 * Swift presents `PHPickerViewController` (IMAGE / IMAGE_VIDEO) or
 * `UIDocumentPickerViewController` (FILE / FOLDER) / `UIDocumentPickerViewController`
 * for export (ACTION_CREATE_DOCUMENT equivalent). On completion Swift calls
 * [IosFilePickerCallback.onPickResult] / [IosFilePickerCallback.onExportResult],
 * which send `PickFileResultEvent` / `ExportFileResultEvent` back to commonMain.
 *
 * Call [register] once at app startup (from [com.ismartcoding.plain.initIosApp]).
 */
object IosFilePickerEvents {
    private var pickJob: Job? = null
    private var exportJob: Job? = null

    fun register() {
        if (pickJob?.isActive == true) return
        pickJob = receiveEventHandler<PickFileEvent> { event ->
            val picker = IosPlatformRegistry.filePicker()
            if (picker == null) {
                LogCat.w("IosFilePickerEvents: no file picker registered, dropping PickFileEvent")
                return@receiveEventHandler
            }
            LogCat.d("IosFilePickerEvents: pickFile tag=${event.tag} type=${event.type} multiple=${event.multiple}")
            picker.pickFile(event.tag.name, event.type.name, event.multiple)
        }
        exportJob = receiveEventHandler<ExportFileEvent> { event ->
            val picker = IosPlatformRegistry.filePicker()
            if (picker == null) {
                LogCat.w("IosFilePickerEvents: no file picker registered, dropping ExportFileEvent")
                return@receiveEventHandler
            }
            LogCat.d("IosFilePickerEvents: exportFile type=${event.type} fileName=${event.fileName}")
            picker.exportFile(event.type.name, event.fileName)
        }
    }
}
