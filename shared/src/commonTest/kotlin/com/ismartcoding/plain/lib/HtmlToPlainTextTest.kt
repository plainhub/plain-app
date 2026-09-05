package com.ismartcoding.plain.lib

import com.ismartcoding.plain.lib.extensions.htmlToPlainText
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlToPlainTextTest {
    @Test
    fun decodes_numeric_char_refs() {
        assertEquals("Here’s how", "Here&#8217;s how".htmlToPlainText())
        assertEquals("A ’ B", "A &#x2019; B".htmlToPlainText())
        assertEquals("end…", "end&#8230;".htmlToPlainText())
    }

    @Test
    fun strips_tags_and_decodes_named_entities() {
        assertEquals("Title", "<b>Title</b>".htmlToPlainText())
        assertEquals("a & b \"q\"", "a &amp; b &quot;q&quot;".htmlToPlainText())
    }

    @Test
    fun double_encoded_refs_decode_exactly_once() {
        assertEquals("&#8217;", "&amp;#8217;".htmlToPlainText())
    }
}
