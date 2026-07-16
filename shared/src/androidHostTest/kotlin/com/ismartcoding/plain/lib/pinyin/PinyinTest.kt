package com.ismartcoding.plain.lib.pinyin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [Pinyin] — pure-Kotlin Chinese-to-Pinyin conversion.
 *
 * `Pinyin.toPinyin(c: Char)` and `Pinyin.isChinese(c)` are stateless and can
 * be tested without initialization. The String overload is tested both with
 * and without a custom dictionary to cover the segmentation path used by
 * `String.toSortName()` (contacts/sort-key generation).
 */
class PinyinTest {

    // ---------- isChinese ----------

    @Test
    fun isChinese_trueForCommonCJK() {
        assertTrue(Pinyin.isChinese('中'))
        assertTrue(Pinyin.isChinese('国'))
        assertTrue(Pinyin.isChinese('人'))
        assertTrue(Pinyin.isChinese('你'))
        assertTrue(Pinyin.isChinese('好'))
    }

    @Test
    fun isChinese_falseForAscii() {
        assertFalse(Pinyin.isChinese('a'))
        assertFalse(Pinyin.isChinese('Z'))
        assertFalse(Pinyin.isChinese('0'))
        assertFalse(Pinyin.isChinese(' '))
        assertFalse(Pinyin.isChinese('!'))
    }

    @Test
    fun isChinese_falseForOtherScripts() {
        // Japanese Hiragana, Korean Hangul, Cyrillic — not in CJK Unified Ideographs
        assertFalse(Pinyin.isChinese('あ')) // hiragana
        assertFalse(Pinyin.isChinese('가')) // hangul
        assertFalse(Pinyin.isChinese('Я')) // cyrillic
    }

    @Test
    fun isChinese_trueForIdeographicNumberZero() {
        // U+3007 〇 — the Chinese zero, explicitly recognized as Chinese by Pinyin.
        assertTrue(Pinyin.isChinese('〇'))
    }

    // ---------- toPinyin(Char) ----------

    @Test
    fun toPinyinChar_returnsUppercasePinyinForChinese() {
        assertEquals("ZHONG", Pinyin.toPinyin('中'))
        assertEquals("GUO", Pinyin.toPinyin('国'))
        assertEquals("NI", Pinyin.toPinyin('你'))
        assertEquals("HAO", Pinyin.toPinyin('好'))
    }

    @Test
    fun toPinyinChar_returnsItselfForNonChinese() {
        assertEquals("a", Pinyin.toPinyin('a'))
        assertEquals("Z", Pinyin.toPinyin('Z'))
        assertEquals("0", Pinyin.toPinyin('0'))
        assertEquals(" ", Pinyin.toPinyin(' '))
        assertEquals("!", Pinyin.toPinyin('!'))
    }

    // ---------- toPinyin(String) without init ----------

    @Test
    fun toPinyinString_asciiPassesThrough() {
        assertEquals("hello", Pinyin.toPinyin("hello"))
    }

    @Test
    fun toPinyinString_asciiWithSeparator() {
        assertEquals("h,e,l,l,o", Pinyin.toPinyin("hello", separator = ","))
    }

    @Test
    fun toPinyinString_chineseConvertedCharByChar() {
        // Without a dict, segmentation is per-character.
        assertEquals("ZHONGGUO", Pinyin.toPinyin("中国"))
    }

    @Test
    fun toPinyinString_mixedAsciiAndChinese() {
        assertEquals("helloZHONGGUO", Pinyin.toPinyin("hello中国"))
    }

    @Test
    fun toPinyinString_emptyInput() {
        assertEquals("", Pinyin.toPinyin(""))
    }

    @Test
    fun toPinyinString_chineseWithSeparator() {
        assertEquals("ZHONG,GUO", Pinyin.toPinyin("中国", separator = ","))
    }

    // ---------- toPinyin(String) with custom dict ----------

    @Test
    fun toPinyinString_withDict() {
        // Build a small dict mapping the two-char word "中国" to a single pinyin token.
        val dict = object : PinyinMapDict() {
            override fun mapping(): Map<String, Array<String>> = mapOf("中国" to arrayOf("ZHONGGUO"))
        }
        Pinyin.init(Pinyin.newConfig().with(dict))
        try {
            assertEquals("ZHONGGUO", Pinyin.toPinyin("中国"))
        } finally {
            // Reset to avoid leaking state between tests.
            Pinyin.init(null)
        }
    }

    @Test
    fun toPinyinString_withWordRulesOverride() {
        // Multi-char word rules are applied: the Engine matches "中国" as a
        // 2-char word from the rules dict and returns the override.
        // (Single-char-per-character rules have non-obvious semantics — the
        // Engine only applies them to chars that the selector picks up as
        // standalone words, which depends on dict interaction — so we only
        // test the well-defined multi-char word rule path here.)
        val rules = PinyinRules().add("中国", "ZHONGGUO1")
        Pinyin.init(null)
        try {
            assertEquals("ZHONGGUO1", Pinyin.toPinyin("中国", rules = rules))
        } finally {
            Pinyin.init(null)
        }
    }

    // ---------- Config ----------

    @Test
    fun newConfigStartsEmpty() {
        val config = Pinyin.newConfig()
        assertTrue(config.pinyinDicts.isEmpty())
        assertTrue(config.valid())
    }

    @Test
    fun configWithAddsDict() {
        val dict = object : PinyinMapDict() {
            override fun mapping(): Map<String, Array<String>> = mapOf("test" to arrayOf("TEST"))
        }
        val config = Pinyin.newConfig().with(dict)
        assertEquals(1, config.pinyinDicts.size)
        assertTrue(config.pinyinDicts.contains(dict))
    }

    @Test
    fun configWithDoesNotDuplicateSameDict() {
        val dict = object : PinyinMapDict() {
            override fun mapping(): Map<String, Array<String>> = mapOf("test" to arrayOf("TEST"))
        }
        val config = Pinyin.newConfig().with(dict).with(dict)
        assertEquals(1, config.pinyinDicts.size)
    }

    // ---------- add() / init(null) ----------

    @Test
    fun addIgnoresEmptyDict() {
        val emptyDict = object : PinyinMapDict() {
            override fun mapping(): Map<String, Array<String>> = emptyMap()
        }
        Pinyin.init(null)
        try {
            Pinyin.add(emptyDict)
            // Empty dict should not initialize the engine.
            // Per-char conversion still works (returns to original behavior).
            assertEquals("ZHONGGUO", Pinyin.toPinyin("中国"))
        } finally {
            Pinyin.init(null)
        }
    }

    @Test
    fun initNullClearsState() {
        val dict = object : PinyinMapDict() {
            override fun mapping(): Map<String, Array<String>> = mapOf("中国" to arrayOf("CUSTOM"))
        }
        Pinyin.init(Pinyin.newConfig().with(dict))
        Pinyin.init(null)
        // After clearing, "中国" should fall back to per-char conversion.
        assertEquals("ZHONGGUO", Pinyin.toPinyin("中国"))
    }
}
