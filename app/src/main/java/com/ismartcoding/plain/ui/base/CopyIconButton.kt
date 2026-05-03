package com.ismartcoding.plain.ui.base

import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.ui.helpers.DialogHelper

@Composable
fun CopyIconButton(
    text: String,
    clipLabel: String,
    modifier: Modifier = Modifier,
    icon: Int = R.drawable.copy,
    contentDescription: String = stringResource(R.string.copy_text),
    copiedMessage: String = text,
    onCopied: (() -> Unit)? = null,
) {
    PIconButton(
        icon = icon,
        modifier = modifier,
        contentDescription = contentDescription,
        click = {
            val clip = ClipData.newPlainText(clipLabel, text)
            clipboardManager.setPrimaryClip(clip)
            DialogHelper.showTextCopiedMessage(copiedMessage)
            onCopied?.invoke()
        },
    )
}
