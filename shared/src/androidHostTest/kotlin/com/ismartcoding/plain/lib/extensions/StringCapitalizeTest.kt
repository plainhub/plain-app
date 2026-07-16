package com.ismartcoding.plain.lib.extensions

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [String.capitalize] — a pure-Kotlin replacement for the
 * deprecated `kotlin.text.capitalize` extension. Used by contact sort-key
 * generation and feed title formatting.
 */
class StringCapitalizeTest {

    @Test
    fun emptyString() {
        assertEquals("", "".capitalize())
    }

    @Test
    fun lowerCaseFirstChar() {
        assertEquals("Hello", "hello".capitalize())
    }

    @Test
    fun upperCaseFirstCharUnchanged() {
        assertEquals("Hello", "Hello".capitalize())
    }

    @Test
    fun onlyFirstCharAffected() {
        assertEquals("Hello world", "hello world".capitalize())
    }

    @Test
    fun alreadyCapitalizedRemainsUnchanged() {
        assertEquals("World", "World".capitalize())
    }

    @Test
    fun singleCharacter() {
        assertEquals("A", "a".capitalize())
        assertEquals("Z", "Z".capitalize())
    }

    @Test
    fun nonLetterFirstCharUnchanged() {
        assertEquals("123abc", "123abc".capitalize())
        assertEquals("!hello", "!hello".capitalize())
    }

    @Test
    fun chineseFirstCharUnchanged() {
        // Chinese characters don't have titlecase variants — remain as-is.
        assertEquals("中国", "中国".capitalize())
    }

    @Test
    fun germanSharpS() {
        // ß.titlecase() is "Ss" in Unicode — verify behavior is at least consistent.
        val result = "straße".capitalize()
        // First char 's' -> 'S', rest unchanged
        assertEquals("Straße", result)
    }

    @Test
    fun preservesWhitespace() {
        // The first char is a space — space.isLowerCase() is false, so the
        // implementation leaves it untouched and the rest of the string
        // unchanged too.
        assertEquals(" hello", " hello".capitalize())
    }
}
