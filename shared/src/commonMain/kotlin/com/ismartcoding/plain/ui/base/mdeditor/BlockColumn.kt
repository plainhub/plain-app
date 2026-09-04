package com.ismartcoding.plain.ui.base.mdeditor

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import com.ismartcoding.plain.ui.base.mdeditor.blocks.BlockEditorState
import com.ismartcoding.plain.ui.theme.PlainTheme

/**
 * Scrollable column that stacks the editor's blocks and overlays the persistent
 * editing field on the active block's slot.
 *
 * Child order is part of the contract: one child per block in document order,
 * then the editing field LAST. The field must keep the final composition slot —
 * moving it between block slots detaches its modifier nodes, which cancels the
 * IME session and bounces the keyboard.
 *
 * The reported height is never smaller than the scroll viewport: a short column
 * would be vertically centered by the scroll container, floating an empty note's
 * caret to mid-screen. Filling the viewport keeps short documents top-aligned.
 */
@Composable
internal fun BlockColumn(
    editor: BlockEditorState,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN),
        content = content,
    ) { measurables, constraints ->
        val hasField = measurables.size == editor.blocks.size + 1
        val activeIdx = if (hasField) {
            val activeId = editor.focusedBlockId.value
            editor.blocks.indexOfFirst { it.id == activeId }
        } else {
            -1
        }

        val childConstraints = constraints.copy(minHeight = 0)
        val field = if (hasField) measurables.last().measure(childConstraints) else null

        var contentY = 0
        var fieldY = 0
        val placeables = measurables.dropLast(if (hasField) 1 else 0).mapIndexed { i, m ->
            val p = m.measure(childConstraints)
            if (i == activeIdx) fieldY = contentY
            contentY += p.height
            p
        }

        // verticalScroll measures its child with unbounded height, so the viewport
        // height comes from the scroll state, not from the incoming constraints
        val height = maxOf(contentY, scrollState.viewportSize, viewportHeight(constraints))
        layout(constraints.maxWidth, height) {
            var y = 0
            placeables.forEach { p ->
                p.place(0, y)
                y += p.height
            }
            field?.place(0, fieldY)
        }
    }
}

private fun viewportHeight(constraints: Constraints): Int =
    if (constraints.maxHeight == Constraints.Infinity) 0 else constraints.maxHeight
