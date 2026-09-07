package com.ismartcoding.plain.lib.rss

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Character-data semantics of the XML reader: plain text, CDATA sections,
 * comments and processing instructions interleave freely in a valid document
 * and must surface as one logical text node. Real feeds wrap CDATA in
 * whitespace, e.g. geekpark.net/rss emits
 * `<title>\n        <![CDATA[...]]>\n      </title>`.
 */
class RssParserCDataTest {
    @Test
    fun whitespace_wrapped_cdata_title_and_description_parse_fully() = runBlocking {
        val xml =
            "<rss version=\"2.0\"><channel><title>极客公园</title>" +
                "<item><title>\n        <![CDATA[雷军：小米汽车销量突破 80 万]]>\n      </title>" +
                "<link>http://www.geekpark.net/news/369884</link>" +
                "<description>\n        <![CDATA[<p>hello</p>]]>\n      </description></item>" +
                "</channel></rss>"
        val channel = RssParser().parse(xml)
        assertEquals("极客公园", channel.title)
        assertEquals("雷军：小米汽车销量突破 80 万", channel.items[0].title)
        assertEquals("<p>hello</p>", channel.items[0].description)
    }

    @Test
    fun cdata_adjacent_to_text_merges_into_one_text_node() = runBlocking {
        val xml = "<rss version=\"2.0\"><channel><title>abc<![CDATA[def]]>ghi</title></channel></rss>"
        assertEquals("abcdefghi", RssParser().parse(xml).title)
    }

    @Test
    fun entities_decode_in_text_but_stay_raw_in_cdata() = runBlocking {
        val xml =
            "<rss version=\"2.0\"><channel>" +
                "<title>A &amp; <![CDATA[x &amp; y &#8217;]]> B</title></channel></rss>"
        assertEquals("A & x &amp; y &#8217; B", RssParser().parse(xml).title)
    }

    @Test
    fun comments_and_processing_instructions_inside_text_do_not_split_it() = runBlocking {
        val xml =
            "<rss version=\"2.0\"><channel>" +
                "<title>abc<!-- c -->def<?pi i?>ghi</title></channel></rss>"
        assertEquals("abcdefghi", RssParser().parse(xml).title)
    }

    @Test
    fun comment_before_cdata_still_yields_cdata_text() = runBlocking {
        val xml = "<rss version=\"2.0\"><channel><title><!-- note --><![CDATA[x]]></title></channel></rss>"
        assertEquals("x", RssParser().parse(xml).title)
    }

    @Test
    fun self_closing_title_yields_empty_string() = runBlocking {
        val xml = "<rss version=\"2.0\"><channel><title/><item><title/></item></channel></rss>"
        val channel = RssParser().parse(xml)
        assertEquals("", channel.title)
        assertEquals("", channel.items[0].title)
    }

    @Test
    fun positions_stay_aligned_after_merged_text() = runBlocking {
        val xml =
            "<rss version=\"2.0\"><channel><title>C</title>" +
                "<item><title>\n<![CDATA[t1]]>\n</title><link>l1</link>" +
                "<pubDate>Mon, 07 Sep 2026 08:00:00 +0800</pubDate></item>" +
                "<item><title>t2</title><link>l2</link>" +
                "<pubDate>Tue, 08 Sep 2026 08:00:00 +0800</pubDate></item>" +
                "</channel></rss>"
        val channel = RssParser().parse(xml)
        assertEquals("t1", channel.items[0].title)
        assertEquals("l1", channel.items[0].link)
        assertEquals("Mon, 07 Sep 2026 08:00:00 +0800", channel.items[0].pubDate)
        assertEquals("t2", channel.items[1].title)
        assertEquals("l2", channel.items[1].link)
    }

    @Test
    fun atom_whitespace_wrapped_cdata_title_parses_fully() = runBlocking {
        val xml =
            "<feed xmlns=\"http://www.w3.org/2005/Atom\"><title>F</title>" +
                "<entry><title>\n  <![CDATA[atom entry]]>\n  </title></entry></feed>"
        val channel = RssParser().parse(xml)
        assertEquals("F", channel.title)
        assertEquals("atom entry", channel.items[0].title)
    }
}
