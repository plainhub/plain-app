package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.theme.PlainTheme

@Composable
fun PCard(
    content: @Composable () -> Unit,
) {
    Column(
        modifier = PlainTheme.getCardModifier(0, 1),
    ) {
        content()
    }
}