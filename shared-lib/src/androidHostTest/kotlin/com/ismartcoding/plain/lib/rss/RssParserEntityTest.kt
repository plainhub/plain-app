package com.ismartcoding.plain.lib.rss

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RssParserEntityTest {
    @Test
    fun numeric_char_refs_in_titles_are_decoded() = runBlocking {
        val xml =
            "<rss version=\"2.0\"><channel><title>Tech&#8217;s Blog</title>" +
                "<item><title>Here&#8217;s how iPhone Ultra&#8217;s form factor could compare</title>" +
                "<link>https://example.com/a</link><description>hi</description></item>" +
                "</channel></rss>"
        val channel = RssParser().parse(xml)
        assertEquals("Tech’s Blog", channel.title)
        assertEquals("Here’s how iPhone Ultra’s form factor could compare", channel.items[0].title)
    }

    @Test
    fun hex_refs_decode_and_amp_encoded_refs_decode_once() = runBlocking {
        val xml =
            "<rss version=\"2.0\"><channel><title>A &#x2019; B</title>" +
                "<item><title>x &amp;amp; y</title></item></channel></rss>"
        val channel = RssParser().parse(xml)
        assertEquals("A ’ B", channel.title)
        assertEquals("x &amp; y", channel.items[0].title)
    }

    @Test
    fun cdata_content_stays_raw() = runBlocking {
        val xml =
            "<rss version=\"2.0\"><channel><title><![CDATA[Raw &#8217; CDATA]]></title></channel></rss>"
        val channel = RssParser().parse(xml)
        assertEquals("Raw &#8217; CDATA", channel.title)
    }
}
