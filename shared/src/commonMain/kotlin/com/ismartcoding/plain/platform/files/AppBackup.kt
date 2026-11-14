package com.ismartcoding.plain.platform.files

import com.ismartcoding.plain.rust.native.RustNativeApi


import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.platform.system.getDeviceCapabilities

import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.ui.components.DialogHelper
import kotlinx.coroutines.CancellationException
import com.ismartcoding.plain.ui.app.MainViewModel

fun backup(uriStr: String) = exportBackup { path ->
    check(copyFileToUri(path, uriStr)) { "Unable to write selected backup file" }
    getFileNameFromUri(uriStr) ?: uriStr
}

fun backupToFile(fileName: String) = exportBackup { path ->
    saveFileToDownloads(path, fileName).also { check(it.isNotEmpty()) { "Unable to save backup" } }
}

private fun exportBackup(save: suspend (String) -> String) {
    coMain {
        DialogHelper.showLoading()
        var archive: String? = null
        try {
            val path = RustNativeApi.exportAppBackup()
            archive = path
            val destination = save(path)
            DialogHelper.showConfirmDialog("", LocaleHelper.getStringFAsync(Res.string.exported_to, destination))
        } catch (canceled: CancellationException) {
            throw canceled
        } catch (error: Exception) {
            DialogHelper.showMessage(error.message ?: LocaleHelper.getStringAsync(Res.string.error))
        } finally {
            archive?.let { deleteFileAt(it) }
            DialogHelper.hideLoading()
        }
    }
}

fun restore(uriStr: String) {
    coMain {
        DialogHelper.showLoading()
        try {
            RustNativeApi.stageAppRestore(uriStr)
            val message = LocaleHelper.getStringAsync(Res.string.app_restore_ready)
            if (Capability.APP_RELAUNCH in getDeviceCapabilities()) {
                DialogHelper.showConfirmDialog("", message) {
                    MainViewModel.relaunchApp()
                }
            } else {
                DialogHelper.showConfirmDialog("", message)
            }
        } catch (canceled: CancellationException) {
            throw canceled
        } catch (error: Exception) {
            DialogHelper.showMessage(error.message ?: LocaleHelper.getStringAsync(Res.string.error))
        } finally {
            DialogHelper.hideLoading()
        }
    }
}
