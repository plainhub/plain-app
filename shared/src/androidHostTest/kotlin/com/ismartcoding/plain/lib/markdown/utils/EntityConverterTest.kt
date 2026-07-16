package com.ismartcoding.plain.lib.markdown.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [EntityConverter] — used by the markdown renderer to decode
 * HTML entities (named, decimal, hex) and optionally process backslash escapes.
 *
 * The implementation is based on JetBrains' markdown library EntityConverter
 * with the HTML-focused escaping branch removed (multiplatform-markdown-renderer
 * PR #222).
 */
class EntityConverterTest {

    // ---------- named entities ----------

    @Test
    fun namedEntity_amp() {
        assertEquals("&", EntityConverter.replaceEntities("&amp;", processEntities = true, processEscapes = false))
    }

    @Test
    fun namedEntity_lt_gt() {
        assertEquals("<", EntityConverter.replaceEntities("&lt;", true, false))
        assertEquals(">", EntityConverter.replaceEntities("&gt;", true, false))
    }

    @Test
    fun namedEntity_quot() {
        assertEquals("\"", EntityConverter.replaceEntities("&quot;", true, false))
    }

    @Test
    fun namedEntity_nbsp() {
        assertEquals("\u00A0", EntityConverter.replaceEntities("&nbsp;", true, false))
    }

    @Test
    fun multipleNamedEntitiesInOneString() {
        assertEquals("&<>\"", EntityConverter.replaceEntities("&amp;&lt;&gt;&quot;", true, false))
    }

    // ---------- numeric (decimal) entities ----------

    @Test
    fun decimalEntity_basicAscii() {
        assertEquals("A", EntityConverter.replaceEntities("&#65;", true, false))
    }

    @Test
    fun decimalEntity_chinese() {
        // U+4E2D = 中
        assertEquals("中", EntityConverter.replaceEntities("&#20013;", true, false))
    }

    // ---------- hex entities ----------

    @Test
    fun hexEntity_lowercaseX() {
        assertEquals("B", EntityConverter.replaceEntities("&#x42;", true, false))
    }

    @Test
    fun hexEntity_uppercaseX() {
        assertEquals("B", EntityConverter.replaceEntities("&#X42;", true, false))
    }

    @Test
    fun hexEntity_chinese() {
        assertEquals("中", EntityConverter.replaceEntities("&#x4E2D;", true, false))
    }

    // ---------- invalid / unknown entities ----------

    @Test
    fun unknownNamedEntity_fallsBackToLiteralAmp() {
        // Entities.map doesn't contain &notreal; — output prepends '&' back.
        val result = EntityConverter.replaceEntities("&notreal;", true, false)
        // The implementation prepends '&' and the rest of the matched value (minus the leading '&')
        assertEquals("&notreal;", result)
    }

    @Test
    fun unescapedAmpersandPreserved() {
        // '&' not followed by a valid entity pattern stays as-is.
        assertEquals("a & b", EntityConverter.replaceEntities("a & b", true, false))
    }

    @Test
    fun malformedNumericEntityPreserved() {
        // &#abc; — invalid decimal — should not crash, falls back to literal.
        val result = EntityConverter.replaceEntities("&#abc;", true, false)
        // Implementation: code is null, returns "&" + match.value.substring(1)
        assertEquals("&#abc;", result)
    }

    // ---------- processEntities = false ----------

    @Test
    fun processEntitiesFalse_leavesNamedEntitiesAlone() {
        assertEquals("&amp;", EntityConverter.replaceEntities("&amp;", processEntities = false, processEscapes = false))
    }

    @Test
    fun processEntitiesFalse_leavesNumericEntitiesAlone() {
        assertEquals("&#65;", EntityConverter.replaceEntities("&#65;", false, false))
        assertEquals("&#x42;", EntityConverter.replaceEntities("&#x42;", false, false))
    }

    // ---------- quote/amp/lt/gt always processed ----------

    @Test
    fun quoteAlwaysReplacedEvenWhenProcessEntitiesFalse() {
        // The regex group 4 (["&<>]) is always matched regardless of processEntities flag.
        assertEquals("&", EntityConverter.replaceEntities("&", false, false))
        assertEquals("<", EntityConverter.replaceEntities("<", false, false))
        assertEquals(">", EntityConverter.replaceEntities(">", false, false))
        assertEquals("\"", EntityConverter.replaceEntities("\"", false, false))
    }

    // ---------- processEscapes ----------

    @Test
    fun processEscapes_decodesBackslashEscape() {
        // \* -> * (only when processEscapes=true)
        assertEquals("*", EntityConverter.replaceEntities("\\*", processEntities = false, processEscapes = true))
    }

    @Test
    fun processEscapes_falsePreservesBackslash() {
        assertEquals("\\*", EntityConverter.replaceEntities("\\*", false, false))
    }

    @Test
    fun processEscapes_decodesMultiplePunctuationEscapes() {
        val input = "\\!\\\"\\#\\$\\%\\&\\'\\(\\)\\*\\+\\,\\-\\.\\/\\:\\;\\<\\=\\>\\?\\@\\[\\\\\\]\\^\\_\\`\\{\\|\\}\\~"
        val result = EntityConverter.replaceEntities(input, processEntities = false, processEscapes = true)
        assertEquals("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~", result)
    }

    @Test
    fun processEscapes_doesNotDecodeBackslashBeforeNonPunctuation() {
        // \n is NOT in the escape set — kept as-is
        val result = EntityConverter.replaceEntities("\\n", processEntities = false, processEscapes = true)
        assertEquals("\\n", result)
    }

    // ---------- combined behavior ----------

    @Test
    fun mixedEntitiesAndText() {
        val input = "Price &amp; tax: &#36;5 &lt; &#36;10"
        assertEquals("Price & tax: $5 < $10", EntityConverter.replaceEntities(input, true, false))
    }

    @Test
    fun emptyInputReturnsEmpty() {
        assertEquals("", EntityConverter.replaceEntities("", true, true))
    }

    @Test
    fun noEntitiesReturnsInputUnchanged() {
        assertEquals("plain text without entities", EntityConverter.replaceEntities("plain text without entities", true, false))
    }

    @Test
    fun entitiesWithSurroundingMarkdown() {
        // What the markdown renderer typically sees after lexing
        val input = "1 &lt; 2 &amp;&amp; 3 &gt; 2"
        assertEquals("1 < 2 && 3 > 2", EntityConverter.replaceEntities(input, true, false))
    }

    @Test
    fun preservesNonEntityText() {
        val input = "Visit &amp; subscribe for &lt;tips&gt;"
        assertEquals("Visit & subscribe for <tips>", EntityConverter.replaceEntities(input, true, false))
    }
}
