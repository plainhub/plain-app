package com.ismartcoding.plain.lib.ahocorasick.trie

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for [Trie] — the pure-Kotlin multi-pattern matcher backing the
 * Pinyin engine's dictionary lookup.
 *
 * The matcher is a naive O(n·m) per-position scan (not a true Aho-Corasick
 * automaton), so tests focus on correctness of the emit set: prefix matches,
 * overlapping keywords, duplicate keywords, empty input, and Unicode.
 */
class TrieTest {

    @Test
    fun emptyTextProducesNoEmits() {
        val trie = Trie().apply { addKeyword("hello") }
        assertTrue(trie.parseText("").isEmpty())
    }

    @Test
    fun noKeywordsProducesNoEmits() {
        val trie = Trie()
        assertTrue(trie.parseText("anything").isEmpty())
    }

    @Test
    fun singleKeywordMatchedAtStart() {
        val trie = Trie().apply { addKeyword("hello") }
        val emits = trie.parseText("hello world")
        assertEquals(1, emits.size)
        val emit = emits.first()
        assertEquals(0, emit.start)
        assertEquals("hello", emit.keyword)
        assertEquals(5, emit.end)
        assertEquals(5, emit.size())
    }

    @Test
    fun singleKeywordMatchedInMiddle() {
        val trie = Trie().apply { addKeyword("world") }
        val emits = trie.parseText("hello world!")
        assertEquals(1, emits.size)
        assertEquals(6, emits.first().start)
    }

    @Test
    fun multipleOccurrences() {
        val trie = Trie().apply { addKeyword("ab") }
        val emits = trie.parseText("ab ab ab")
        assertEquals(3, emits.size)
        assertEquals(listOf(0, 3, 6), emits.map { it.start })
    }

    @Test
    fun multipleKeywords() {
        val trie = Trie().apply {
            addKeyword("he")
            addKeyword("she")
            addKeyword("his")
            addKeyword("hers")
        }
        val emits = trie.parseText("ushers")
        // Matches: "she" (1), "he" (2), "hers" (2)
        val keywords = emits.map { it.keyword }.toSet()
        assertTrue("she" in keywords)
        assertTrue("he" in keywords)
        assertTrue("hers" in keywords)
    }

    @Test
    fun overlappingPrefixes() {
        val trie = Trie().apply {
            addKeyword("a")
            addKeyword("ab")
            addKeyword("abc")
        }
        val emits = trie.parseText("abc")
        // All three prefixes match starting at position 0.
        val keywords = emits.map { it.keyword }.toSet()
        assertEquals(setOf("a", "ab", "abc"), keywords)
    }

    @Test
    fun duplicateKeywordAddedOnce() {
        val trie = Trie().apply {
            addKeyword("cat")
            addKeyword("cat")
        }
        assertEquals(1, trie.keywords().size)
        val emits = trie.parseText("cat cat")
        // Only one keyword in the trie, but it matches twice in the text.
        assertEquals(2, emits.size)
    }

    @Test
    fun emptyKeywordIgnored() {
        val trie = Trie().apply { addKeyword("") }
        assertTrue(trie.keywords().isEmpty())
        assertTrue(trie.parseText("anything").isEmpty())
    }

    @Test
    fun unicodeKeyword() {
        val trie = Trie().apply { addKeyword("中国") }
        val emits = trie.parseText("我爱中国")
        assertEquals(1, emits.size)
        assertEquals("中国", emits.first().keyword)
        assertEquals(2, emits.first().start)
    }

    @Test
    fun noMatchReturnsEmpty() {
        val trie = Trie().apply { addKeyword("xyz") }
        assertTrue(trie.parseText("abcdef").isEmpty())
    }

    @Test
    fun caseSensitiveMatch() {
        val trie = Trie().apply { addKeyword("Hello") }
        // Trie is case-sensitive — "hello" should NOT match keyword "Hello"
        val emits = trie.parseText("hello Hello")
        assertEquals(1, emits.size)
        assertEquals(6, emits.first().start)
    }

    @Test
    fun keywordsListReflectsInsertionOrder() {
        val trie = Trie().apply {
            addKeyword("first")
            addKeyword("second")
            addKeyword("third")
        }
        assertEquals(listOf("first", "second", "third"), trie.keywords().toList())
    }

    @Test
    fun emitPositionsCorrectAcrossLineBreaks() {
        val trie = Trie().apply { addKeyword("foo") }
        val text = "a\nfoo\nfoo"
        val emits = trie.parseText(text).toList()
        assertEquals(2, emits.size)
        assertEquals(2, emits[0].start)
        assertEquals(6, emits[1].start)
    }
}
