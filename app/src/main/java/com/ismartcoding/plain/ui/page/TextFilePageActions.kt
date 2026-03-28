package com.ismartcoding.plain.ui.page

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.TextFileType
import com.ismartcoding.plain.helpers.AppLogHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.ActionButtonMore
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.TextFileViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import kotlinx.coroutines.delay
import android.content.Context

@Composable
internal fun RowScope.TextFilePageActions(
    textFileVM: TextFileViewModel,
    type: String,
    path: String,
    isSaving: Boolean,
    rotation: Float,
    scope: CoroutineScope,
    context: Context,
    onSavingChanged: (Boolean) -> Unit,
) {
    if (!textFileVM.isEditorReady.value) return

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    if (textFileVM.readOnly.value) {
        if (type != TextFileType.APP_LOG.name && !textFileVM.isExternalFile.value) {
            PIconButton(
                icon = R.drawable.square_pen,
                contentDescription = stringResource(R.string.edit),
                tint = MaterialTheme.colorScheme.onSurface,
            ) {
                textFileVM.enterEditMode()
            }
        }
    } else {
        PIconButton(
            icon = R.drawable.save,
            contentDescription = stringResource(R.string.save),
            tint = if (isSaving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.rotate(rotation),
        ) {
            scope.launch {
                if (textFileVM.isExternalFile.value) {
                    DialogHelper.showMessage(R.string.not_supported_error)
                    return@launch
                }
                keyboardController?.hide()
                focusManager.clearFocus()
                onSavingChanged(true)
                DialogHelper.showLoading()
                withIO { File(path).writeText(textFileVM.content.value) }
                textFileVM.oldContent.value = textFileVM.content.value
                context.scanFileByConnection(path)
                DialogHelper.hideLoading()
                delay(600)
                onSavingChanged(false)
            }
        }
    }
    if (setOf(TextFileType.APP_LOG.name, TextFileType.CHAT.name).contains(type)) {
        PIconButton(
            icon = R.drawable.wrap_text,
            contentDescription = stringResource(R.string.wrap_content),
            tint = MaterialTheme.colorScheme.onSurface,
        ) {
            textFileVM.toggleWrapContent(context)
        }
        PIconButton(
            icon = R.drawable.share_2,
            contentDescription = stringResource(R.string.share),
            tint = MaterialTheme.colorScheme.onSurface,
        ) {
            if (type == TextFileType.APP_LOG.name) {
                AppLogHelper.export(context)
            } else if (type == TextFileType.CHAT.name) {
                ShareHelper.shareFile(context, File(path))
            }
        }
    } else {
        ActionButtonMore {
            textFileVM.showMoreActions.value = true
        }
    }
}
