package com.ismartcoding.plain.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.delete_forever as ui_drawable_delete_forever

@Composable
fun PSheetPrimaryDeleteAction(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    PSheetPrimaryAction(
        UiRes.drawable.ui_drawable_delete_forever,
        text,
        container = MaterialTheme.colorScheme.errorContainer,
        tint = MaterialTheme.colorScheme.error,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}
