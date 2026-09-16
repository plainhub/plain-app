package com.ismartcoding.plain.lib.codeeditor

/**
 * Horizontal panning state for no-wrap rows. Pure logic, unit-testable.
 *
 * Invariant: 0 <= offsetPx <= maxOverhangPx, where maxOverhangPx is how much the widest
 * content row exceeds the viewport. Rows shift left by offsetPx; short rows simply reveal
 * trailing space. Offsets beyond the bounds are silently clamped so a wider row discovered
 * later (progressive widest-row tracking) never leaves the pan stuck out of range.
 */
class HorizontalPan {

    var offsetPx: Float = 0f
        private set

    var maxOverhangPx: Float = 0f
        private set

    /** Recomputes the overhang bound; returns true if the offset changed. */
    fun updateBounds(contentWidthPx: Float, viewportWidthPx: Float): Boolean {
        val max = (contentWidthPx - viewportWidthPx).coerceAtLeast(0f)
        maxOverhangPx = max
        val clamped = offsetPx.coerceIn(0f, maxOverhangPx)
        if (clamped != offsetPx) {
            offsetPx = clamped
            return true
        }
        return false
    }

    /**
     * Applies a horizontal drag delta in px (positive = finger moved right = content pans
     * left). Returns true if the offset changed.
     */
    fun drag(deltaPx: Float): Boolean {
        val next = (offsetPx - deltaPx).coerceIn(0f, maxOverhangPx)
        if (next == offsetPx) return false
        offsetPx = next
        return true
    }

    fun reset() {
        offsetPx = 0f
    }
}
