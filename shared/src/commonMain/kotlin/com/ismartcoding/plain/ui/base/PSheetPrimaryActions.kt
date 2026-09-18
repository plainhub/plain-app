package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.PlainTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

// Primary bottom-sheet actions: a row of colored disc buttons, replacing
// FlowRow button walls. One or two actions keep four-column slots anchored
// left (rest of the row stays empty); three or more split the width evenly,
// capped at [MAX_PSheetPrimaryActionsPerRow] columns.
// Contract: keep at most 4 actions per card. Anything beyond belongs in the
// sheet's secondary actions card (PSheetActionRow) — overflow here would run
// off-screen (the FileInfoBottomSheet delete-button regression).

internal const val MAX_PSheetPrimaryActionsPerRow = 4

// Column count the row is divided into: 1-2 actions use the full four-column
// grid so they anchor left; 3+ divide evenly, capped at 4.
internal fun actionSlotColumns(actionCount: Int): Int = when {
    actionCount <= 0 -> 1
    actionCount <= 2 -> MAX_PSheetPrimaryActionsPerRow
    else -> minOf(actionCount, MAX_PSheetPrimaryActionsPerRow)
}

// Evenly divides [rowWidthPx] into [actionSlotColumns] slots. The invariant
// that keeps every action on-screen: columns * result <= rowWidthPx.
internal fun actionSlotWidthPx(rowWidthPx: Int, actionCount: Int): Int =
    rowWidthPx / actionSlotColumns(actionCount)

@Composable
private fun PSheetActionsFlowLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val slotWidth = actionSlotWidthPx(constraints.maxWidth, measurables.size)
        val placeables = measurables.map { it.measure(Constraints.fixedWidth(slotWidth)) }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable -> placeable.place(index * slotWidth, 0) }
        }
    }
}

@Composable
fun PSheetPrimaryActionsRow(content: @Composable () -> Unit) {
    PSheetActionsFlowLayout(Modifier.fillMaxWidth().padding(vertical = 12.dp), content = content)
}

@Composable
fun PSheetPrimaryActionsCard(content: @Composable () -> Unit) {
    // No card background: this area holds icon buttons only, no header.
    Box(Modifier.fillMaxWidth().padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PSheetActionsFlowLayout(Modifier.fillMaxWidth().padding(vertical = 12.dp), content = content)
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
    Column(
        modifier = Modifier
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
