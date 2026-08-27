package com.ismartcoding.plain.ui.base

import org.jetbrains.compose.resources.DrawableResource
import com.ismartcoding.plain.i18n.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.platform.setClipboardText

@Composable
fun CopyIconButton(
    text: String,
    clipLabel: String,
    modifier: Modifier = Modifier,
    icon: DrawableResource = Res.drawable.copy,
    contentDescription: String = stringResource(Res.string.copy_text),
    copiedMessage: String = text,
    onCopied: (() -> Unit)? = null,
) {
    PIconButton(
        icon = icon,
        modifier = modifier,
        contentDescription = contentDescription,
        click = {
            setClipboardText(clipLabel, text)
            onCopied?.invoke()
        },
    )
}
