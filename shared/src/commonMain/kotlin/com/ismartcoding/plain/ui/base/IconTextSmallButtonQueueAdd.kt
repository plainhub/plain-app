package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.playlist_add as ui_drawable_playlist_add
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextSmallButtonQueueAdd(onClick: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_playlist_add, text = stringResource(Res.string.add_to_queue), onClick = onClick)
}
