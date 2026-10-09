package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints

internal const val MAX_PSheetPrimaryActionsPerRow = 4

// Column count the row is divided into: 1-2 actions use the full four-column
// grid so they anchor left; 3+ divide evenly, capped at 4.
internal fun actionSlotColumns(actionCount: Int): Int {
    require(actionCount in 0..MAX_PSheetPrimaryActionsPerRow) { "Use secondary sheet actions for more than four actions" }
    return when {
        actionCount <= 0 -> 1
        actionCount <= 2 -> MAX_PSheetPrimaryActionsPerRow
        else -> actionCount
    }
}

// Evenly divides [rowWidthPx] into [actionSlotColumns] slots. The invariant
// that keeps every action on-screen: columns * result <= rowWidthPx.
internal fun actionSlotWidthPx(rowWidthPx: Int, actionCount: Int): Int =
    rowWidthPx / actionSlotColumns(actionCount)

@Composable
internal fun PSheetActionsFlowLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val slotWidth = actionSlotWidthPx(constraints.maxWidth, measurables.size)
        val placeables = measurables.map { it.measure(Constraints.fixedWidth(slotWidth)) }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable -> placeable.placeRelative(index * slotWidth, 0) }
        }
    }
}
