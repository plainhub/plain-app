package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.PlainTheme
import org.jetbrains.compose.resources.DrawableResource

// Secondary bottom-sheet actions: full-width rows inside a card.

@Composable
fun PSheetActionCard(content: @Composable ColumnScope.() -> Unit) {
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        Column(content = content)
    }
}

@Composable
fun PSheetActionRow(
    icon: DrawableResource,
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    click: () -> Unit,
) {
    PListItem(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = click),
        icon = icon,
        title = title,
        action = trailing,
    )
}
