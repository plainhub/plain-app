package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.PlainTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

// Shared components for bottom sheet action areas: a primary row of colored
// disc buttons plus full-width secondary rows, replacing FlowRow button walls.

// Slot width for PSheetPrimaryAction, provided by the primary actions row.
val LocalPSheetActionSlot = staticCompositionLocalOf { Dp.Unspecified }

@Composable
fun PSheetPrimaryActionsRow(content: @Composable RowScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalPSheetActionSlot provides maxWidth / 4) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                content = content,
            )
        }
    }
}

@Composable
fun PSheetPrimaryActionsCard(slots: Int = 4, content: @Composable RowScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        CompositionLocalProvider(LocalPSheetActionSlot provides maxWidth / slots) {
            PCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
fun PSheetPrimaryAction(
    icon: DrawableResource,
    text: String,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    click: () -> Unit,
) {
    val slot = LocalPSheetActionSlot.current
    Column(
        modifier = Modifier
            .width(slot)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = click)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                tint = tint,
            )
        }
        VerticalSpace(4.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

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

@Composable
fun PSheetHeader(
    thumbnail: @Composable () -> Unit,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        thumbnail()
        HorizontalSpace(16.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                VerticalSpace(4.dp)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
