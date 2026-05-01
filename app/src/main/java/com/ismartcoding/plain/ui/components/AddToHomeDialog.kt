package com.ismartcoding.plain.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R
import com.ismartcoding.plain.helpers.MediaShortcutHelper
import com.ismartcoding.plain.ui.base.TextFieldDialog
import java.io.File

@Composable
fun AddToHomeDialog(
    path: String,
    iconRes: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val defaultLabel = remember(path) { File(path).nameWithoutExtension }
    TextFieldDialog(
        title = stringResource(R.string.add_to_home),
        value = defaultLabel,
        description = stringResource(R.string.shortcut_label_hint),
        onDismissRequest = onDismiss,
        onConfirm = { label ->
            MediaShortcutHelper.addToDesktop(context, path, label, iconRes)
            onDismiss()
        },
    )
}
