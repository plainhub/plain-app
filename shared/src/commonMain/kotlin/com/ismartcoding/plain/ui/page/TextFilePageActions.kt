package com.ismartcoding.plain.ui.page

import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.enums.TextFileType
import com.ismartcoding.plain.platform.exportLogsAsync
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.shareFile
import com.ismartcoding.plain.ui.base.ActionButtonMore
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.TextFileViewModel
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
internal fun RowScope.TextFilePageActions(
    textFileVM: TextFileViewModel,
    type: String,
    path: String,
    isSaving: Boolean,
    rotation: Float,
    onSavingChanged: (Boolean) -> Unit,
) {
    val controller = textFileVM.controller
    if (controller.loadState.value !is com.ismartcoding.plain.ui.components.codeeditor.EditorLoadState.Ready) return

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    fun requestEditMode() {
        if (controller.openFileSize > 20L * 1024 * 1024) {
            DialogHelper.showConfirmDialog(
                title = LocaleHelper.getString(Res.string.edit),
                message = LocaleHelper.getString(Res.string.large_file_edit),
                confirmButton = Pair(LocaleHelper.getString(Res.string.ok)) {
                    textFileVM.enterEditMode()
                },
                dismissButton = Pair(LocaleHelper.getString(Res.string.cancel)) {},
            )
        } else textFileVM.enterEditMode()
    }

    if (controller.readOnly.value) {
        PIconButton(
            icon = Res.drawable.search,
            contentDescription = stringResource(Res.string.search),
            tint = MaterialTheme.colorScheme.onSurface,
        ) {
            controller.setSearchVisible(!controller.searchVisible.value)
        }
        PIconButton(
            icon = Res.drawable.wrap_text,
            contentDescription = stringResource(Res.string.wrap_content),
            tint = if (controller.wrapContent.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        ) {
            textFileVM.toggleWrapContent()
        }
        if (type != TextFileType.APP_LOG.name && type != TextFileType.CRASH_REPORT.name && !textFileVM.isExternalFile.value) {
            PIconButton(
                icon = Res.drawable.square_pen,
                contentDescription = stringResource(Res.string.edit),
                tint = MaterialTheme.colorScheme.onSurface,
            ) {
                requestEditMode()
            }
        }
    } else {
        PIconButton(
            icon = Res.drawable.save,
            contentDescription = stringResource(Res.string.save),
            tint = if (isSaving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.rotate(rotation),
        ) {
            scope.launch {
                if (textFileVM.isExternalFile.value) {
                    DialogHelper.showMessage(Res.string.not_supported_error)
                    return@launch
                }
                keyboardController?.hide()
                focusManager.clearFocus()
                onSavingChanged(true)
                DialogHelper.showLoading()
                val result = withIO { controller.saveAsync() }
                DialogHelper.hideLoading()
                if (result.isFailure) {
                    DialogHelper.showErrorDialog(result.exceptionOrNull()?.toString() ?: "save failed")
                }
                delay(600)
                onSavingChanged(false)
            }
        }
    }
    if (setOf(TextFileType.APP_LOG.name, TextFileType.CHAT.name, TextFileType.CRASH_REPORT.name).contains(type)) {
        PIconButton(
            icon = Res.drawable.share_2,
            contentDescription = stringResource(Res.string.share),
            tint = MaterialTheme.colorScheme.onSurface,
        ) {
            when (type) {
                TextFileType.APP_LOG.name -> exportLogsAsync()
                TextFileType.CHAT.name -> shareFile(path)
                TextFileType.CRASH_REPORT.name -> shareFile(path, email = Constants.SUPPORT_EMAIL, subject = "Crash Report - PlainApp")
            }
        }
    } else {
        ActionButtonMore {
            textFileVM.showMoreActions.value = true
        }
    }
}
