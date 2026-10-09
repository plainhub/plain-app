package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.PlainTheme

@Composable
fun PSheetPrimaryActionsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // No card background: this area holds icon buttons only, no header.
    Box(modifier.fillMaxWidth().padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PSheetActionsFlowLayout(modifier.fillMaxWidth().padding(vertical = 12.dp), content = content)
    }
}
