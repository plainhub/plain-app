package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.bottomAppBarContainer

@Composable
fun PBottomAppBar(
    content: @Composable RowScope.() -> Unit,
) {
    BottomAppBar(
        tonalElevation = 2.dp,
        containerColor = MaterialTheme.colorScheme.bottomAppBarContainer(),
        content = content
    )
}