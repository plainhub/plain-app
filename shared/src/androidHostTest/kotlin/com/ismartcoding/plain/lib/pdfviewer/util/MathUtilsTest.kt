package com.ismartcoding.plain.lib.pdfviewer.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [MathUtils] — the libGDX-derived integer/float helpers used
 * by the PDF viewer's tile rendering math. The floor/ceil implementations
 * rely on a "big-enough" additive constant, so we probe the edges of their
 * documented valid range.
 */
class MathUtilsTest {

    // ---------- limit(Int) ----------

    @Test
    fun limitInt_clampsToLowerBound() {
        assertEquals(5, MathUtils.limit(1, 5, 10))
        assertEquals(5, MathUtils.limit(5, 5, 10))
    }

    @Test
    fun limitInt_clampsToUpperBound() {
        assertEquals(10, MathUtils.limit(15, 5, 10))
        assertEquals(10, MathUtils.limit(10, 5, 10))
    }

    @Test
    fun limitInt_preservesValueInRange() {
        assertEquals(7, MathUtils.limit(7, 5, 10))
    }

    // ---------- limit(Float) ----------

    @Test
    fun limitFloat_clampsToLowerBound() {
        assertEquals(5f, MathUtils.limit(1f, 5f, 10f))
    }

    @Test
    fun limitFloat_clampsToUpperBound() {
        assertEquals(10f, MathUtils.limit(15f, 5f, 10f))
    }

    @Test
    fun limitFloat_preservesValueInRange() {
        assertEquals(7.5f, MathUtils.limit(7.5f, 5f, 10f))
    }

    // ---------- max ----------

    @Test
    fun maxInt_returnsInputIfSmallerOrEqual() {
        assertEquals(3, MathUtils.max(3, 10))
        assertEquals(10, MathUtils.max(15, 10))
    }

    @Test
    fun maxFloat_returnsInputIfSmallerOrEqual() {
        assertEquals(3f, MathUtils.max(3f, 10f))
        assertEquals(10f, MathUtils.max(15.5f, 10f))
    }

    // ---------- min ----------

    @Test
    fun minInt_returnsInputIfGreaterOrEqual() {
        // min(number, min): if number < min, return min; else return number.
        // So min(10, 5) returns 10 (10 is not < 5), and min(3, 5) returns 5.
        assertEquals(10, MathUtils.min(10, 5))
        assertEquals(5, MathUtils.min(3, 5))
    }

    @Test
    fun minFloat_returnsInputIfGreaterOrEqual() {
        assertEquals(10f, MathUtils.min(10f, 5f))
        assertEquals(5f, MathUtils.min(3.5f, 5f))
    }

    // ---------- floor / ceil ----------

    @Test
    fun floor_positiveWholeNumber() {
        assertEquals(5, MathUtils.floor(5.0f))
    }

    @Test
    fun floor_positiveFraction() {
        assertEquals(5, MathUtils.floor(5.7f))
        assertEquals(5, MathUtils.floor(5.1f))
    }

    @Test
    fun floor_negativeFraction() {
        assertEquals(-6, MathUtils.floor(-5.1f))
        assertEquals(-6, MathUtils.floor(-5.7f))
    }

    @Test
    fun floor_zero() {
        assertEquals(0, MathUtils.floor(0.0f))
        assertEquals(-1, MathUtils.floor(-0.5f))
    }

    @Test
    fun ceil_positiveWholeNumber() {
        assertEquals(5, MathUtils.ceil(5.0f))
    }

    @Test
    fun ceil_positiveFraction() {
        assertEquals(6, MathUtils.ceil(5.1f))
        assertEquals(6, MathUtils.ceil(5.7f))
    }

    @Test
    fun ceil_negativeFraction() {
        assertEquals(-5, MathUtils.ceil(-5.1f))
        assertEquals(-5, MathUtils.ceil(-5.7f))
    }

    @Test
    fun ceil_zero() {
        assertEquals(0, MathUtils.ceil(0.0f))
        assertEquals(0, MathUtils.ceil(-0.4f))
    }

    @Test
    fun floorVsCeilAgreeWithKotlinMathOnSmallValues() {
        // Cross-check against kotlin.math on values well inside the valid range.
        for (v in listOf(-100f, -1.5f, -0.1f, 0f, 0.5f, 3.14f, 99.9f, 1000f)) {
            assertEquals(kotlin.math.floor(v).toInt(), MathUtils.floor(v), "floor mismatch for $v")
            assertEquals(kotlin.math.ceil(v).toInt(), MathUtils.ceil(v), "ceil mismatch for $v")
        }
    }

    @Test
    fun floorAndCeilAreConsistentAtBoundaries() {
        // floor(x) <= x <= ceil(x) for the implementation's valid range
        for (v in listOf(-100f, -1f, 0f, 1f, 100f, 1000f, 10_000f)) {
            val f = MathUtils.floor(v)
            val c = MathUtils.ceil(v)
            assertTrue(f <= v.toInt() || f == v.toInt(), "floor $f vs $v")
            assertTrue(c >= v.toInt() || c == v.toInt(), "ceil $c vs $v")
        }
    }
}
