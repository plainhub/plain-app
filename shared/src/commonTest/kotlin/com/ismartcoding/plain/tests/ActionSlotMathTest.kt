package com.ismartcoding.plain.tests

import com.ismartcoding.plain.ui.base.MAX_PSheetPrimaryActionsPerRow
import com.ismartcoding.plain.ui.base.actionSlotColumns
import com.ismartcoding.plain.ui.base.actionSlotWidthPx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the PSheetPrimaryActionsCard/Row slot math: one or two actions keep
 * four-column slots anchored left (rest of the row empty); three or more
 * split the width evenly, capped at [MAX_PSheetPrimaryActionsPerRow] columns
 * and at most 4 actions per card (the FileInfoBottomSheet delete-button
 * regression, where a caller-supplied slot count drifted from the real count).
 */
class ActionSlotMathTest {

    @Test
    fun `single action anchors left in a four-column slot`() {
        assertEquals(1080 / 4, actionSlotWidthPx(1080, 1))
    }

    @Test
    fun `two actions anchor left leaving the right half empty`() {
        assertEquals(1080 / 4, actionSlotWidthPx(1080, 2))
    }

    @Test
    fun `three actions split the row evenly`() {
        assertEquals(1080 / 3, actionSlotWidthPx(1080, 3))
    }

    @Test
    fun `four actions divide the row evenly`() {
        assertEquals(1080 / 4, actionSlotWidthPx(1080, 4))
    }

    @Test
    fun `five composed actions keep four-column slots`() {
        assertEquals(1080 / 4, actionSlotWidthPx(1080, 5))
    }

    @Test
    fun `empty row does not divide by zero`() {
        assertEquals(1080, actionSlotWidthPx(1080, 0))
    }

    @Test
    fun `column count is at least one and at most four`() {
        for (count in 0..12) {
            val columns = actionSlotColumns(count)
            assertTrue(columns in 1..MAX_PSheetPrimaryActionsPerRow, "count=$count columns=$columns")
        }
    }

    @Test
    fun `every width and count keeps actions within the row`() {
        for (width in 1..64) {
            for (count in 1..12) {
                val columns = actionSlotColumns(count)
                val slot = actionSlotWidthPx(width, count)
                assertTrue(columns * slot <= width, "count=$count width=$width slot=$slot overflows")
                // Degenerate rows narrower than the column count can only
                // shrink slots to 0; staying in bounds is the contract there.
                if (width >= columns) assertTrue(slot > 0, "count=$count width=$width produced non-positive slot")
            }
        }
    }
}
