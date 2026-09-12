package com.ismartcoding.plain.lib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LrcParserTest {
    @Test
    fun parseBlankReturnsEmpty() {
        assertTrue(LrcParser.parse("").isEmpty())
        assertTrue(LrcParser.parse("   \n  ").isEmpty())
    }

    @Test
    fun parseCentiseconds() {
        val lines = LrcParser.parse("[00:08.50] hello")
        assertEquals(1, lines.size)
        assertEquals(8500L, lines[0].timeMs)
        assertEquals("hello", lines[0].text)
    }

    @Test
    fun parseMilliseconds() {
        val lines = LrcParser.parse("[01:02.345] world")
        assertEquals(62345L, lines[0].timeMs)
    }

    @Test
    fun parseNoFraction() {
        val lines = LrcParser.parse("[01:02] plain")
        assertEquals(62000L, lines[0].timeMs)
    }

    @Test
    fun parseMultipleTimeTags() {
        val lines = LrcParser.parse("[00:01.00][00:05.50] repeat")
        assertEquals(2, lines.size)
        assertEquals(1000L, lines[0].timeMs)
        assertEquals(5500L, lines[1].timeMs)
        assertEquals("repeat", lines[1].text)
    }

    @Test
    fun skipsMetadataAndEnhancedWordTags() {
        val content = "[ti:title]\n[ar:artist]\n[00:01.20]<00:01.20>word <00:02.00>by word"
        val lines = LrcParser.parse(content)
        assertEquals(1, lines.size)
        assertEquals("word by word", lines[0].text)
    }

    @Test
    fun sortsByTime() {
        val lines = LrcParser.parse("[00:10] b\n[00:05] a")
        assertEquals(listOf("a", "b"), lines.map { it.text })
    }

    @Test
    fun skipsEmptyText() {
        val lines = LrcParser.parse("[00:01.00]\n[00:02.00]   \n[00:03.00] real")
        assertEquals(1, lines.size)
        assertEquals("real", lines[0].text)
    }
}
