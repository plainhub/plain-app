package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.ui.base.PIconTextSmallButton

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.delete_forever as ui_drawable_delete_forever
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextSmallButtonDelete(label: StringResource = Res.string.delete, onClick: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_delete_forever, text = stringResource(label), onClick = onClick)
}
