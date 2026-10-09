package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.theme.PlainTheme

@Composable
fun PSheetActionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    PCard(modifier = modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        Column(content = content)
    }
}
