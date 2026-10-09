package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PSheetPrimaryActionsRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    PSheetActionsFlowLayout(modifier.fillMaxWidth().padding(vertical = 12.dp), content = content)
}
