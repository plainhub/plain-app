package com.ismartcoding.plain.lib.rss

import com.ismartcoding.plain.lib.rss.model.RssChannel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertContains

/**
 * Unit tests for RSS DateParser and RssParser.
 * Verifies pure-Kotlin date parsing (RFC822, W3C date-time) and
 * RSS/Atom feed parsing using SimpleXmlReader.
 */
class RssTest {

    // ---------- DateParser ----------

    @Test
    fun parseDate_rfc822_withDayOfWeek() {
        // "Tue, 10 Sep 2024 12:00:00 GMT" → 2024-09-10T12:00:00Z
        val result = DateParser.parseDate("Tue, 10 Sep 2024 12:00:00 GMT")
        assertEquals(1725969600000L, result)
    }

    @Test
    fun parseDate_rfc822_withoutSeconds() {
        // "Tue, 10 Sep 2024 12:00 GMT" — seconds optional in RFC822
        val result = DateParser.parseDate("Tue, 10 Sep 2024 12:00 GMT")
        assertEquals(1725969600000L, result)
    }

    @Test
    fun parseDate_rfc822_twoDigitYear() {
        // "10 Sep 24 12:00:00 GMT" — 2-digit year, 24 → 2024
        val result = DateParser.parseDate("10 Sep 24 12:00:00 GMT")
        assertEquals(1725969600000L, result)
    }

    @Test
    fun parseDate_rfc822_estTimezone() {
        // "Wed, 02 Oct 2024 15:30:00 EST" → 2024-10-02T20:30:00Z
        val result = DateParser.parseDate("Wed, 02 Oct 2024 15:30:00 EST")
        assertEquals(1727901000000L, result)
    }

    @Test
    fun parseDate_rfc822_numericTimezone() {
        // "Tue, 10 Sep 2024 14:00:00 +0200" → 2024-09-10T12:00:00Z
        val result = DateParser.parseDate("Tue, 10 Sep 2024 14:00:00 +0200")
        assertEquals(1725969600000L, result)
    }

    @Test
    fun parseDate_w3cDateTime_withZ() {
        // "2024-09-10T12:00:00Z" → 2024-09-10T12:00:00Z
        val result = DateParser.parseDate("2024-09-10T12:00:00Z")
        assertEquals(1725969600000L, result)
    }

    @Test
    fun parseDate_w3cDateTime_withOffset() {
        // "2024-09-10T14:00:00+02:00" → 2024-09-10T12:00:00Z
        val result = DateParser.parseDate("2024-09-10T14:00:00+02:00")
        assertEquals(1725969600000L, result)
    }

    @Test
    fun parseDate_w3cDateTime_withFractionalSeconds() {
        // "2024-09-10T12:00:00.500Z" → fractional seconds stripped → 2024-09-10T12:00:00Z
        val result = DateParser.parseDate("2024-09-10T12:00:00.500Z")
        assertEquals(1725969600000L, result)
    }

    @Test
    fun parseDate_w3cDateOnly() {
        // "2024-09-10" → 2024-09-10T00:00:00Z
        val result = DateParser.parseDate("2024-09-10")
        assertEquals(1725926400000L, result)
    }

    @Test
    fun parseDate_additionalFormat_spaceSeparator() {
        // "2024-09-10 12:00:00" — space separator, no timezone → UTC
        val result = DateParser.parseDate("2024-09-10 12:00:00")
        assertEquals(1725969600000L, result)
    }

    @Test
    fun parseDate_additionalFormat_withTimezone() {
        // "2024-09-10 14:00:00 +0200"
        val result = DateParser.parseDate("2024-09-10 14:00:00 +0200")
        assertEquals(1725969600000L, result)
    }

    @Test
    fun parseDate_returnsNullForInvalidInput() {
        assertNull(DateParser.parseDate("not a date"))
        assertNull(DateParser.parseDate(""))
    }

    // ---------- RssParser (RSS 2.0) ----------

    private val sampleRssFeed = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/"
             xmlns:content="http://purl.org/rss/1.0/modules/content/"
             xmlns:media="http://search.yahoo.com/mrss/">
            <channel>
                <title>Test Channel Title</title>
                <link>https://example.com</link>
                <description>Test channel description</description>
                <lastBuildDate>Tue, 10 Sep 2024 12:00:00 GMT</lastBuildDate>
                <image>
                    <title>Channel Image</title>
                    <url>https://example.com/logo.png</url>
                    <link>https://example.com</link>
                </image>
                <item>
                    <title>First Article</title>
                    <link>https://example.com/article-1</link>
                    <description>First article description</description>
                    <content:encoded><![CDATA[<p>Full content here</p>]]></content:encoded>
                    <pubDate>Tue, 10 Sep 2024 12:00:00 GMT</pubDate>
                    <guid>guid-1</guid>
                    <dc:creator>Author One</dc:creator>
                    <category>Tech</category>
                    <category>News</category>
                </item>
                <item>
                    <title>Second Article</title>
                    <link>https://example.com/article-2</link>
                    <description>Second article description</description>
                    <pubDate>Wed, 11 Sep 2024 09:30:00 GMT</pubDate>
                    <guid>guid-2</guid>
                    <media:thumbnail url="https://example.com/img2.jpg" />
                </item>
            </channel>
        </rss>
    """.trimIndent()

    @Test
    fun parse_rssFeed_channelFields() = runBlocking {
        val channel = RssParser().parse(sampleRssFeed)

        assertEquals("Test Channel Title", channel.title)
        assertEquals("https://example.com", channel.link)
        assertEquals("Test channel description", channel.description)
        assertEquals("Tue, 10 Sep 2024 12:00:00 GMT", channel.lastBuildDate)
    }

    @Test
    fun parse_rssFeed_channelImage() = runBlocking {
        val channel = RssParser().parse(sampleRssFeed)

        assertNotNull(channel.image)
        assertEquals("Channel Image", channel.image!!.title)
        assertEquals("https://example.com/logo.png", channel.image!!.url)
        assertEquals("https://example.com", channel.image!!.link)
    }

    @Test
    fun parse_rssFeed_itemsCount() = runBlocking {
        val channel = RssParser().parse(sampleRssFeed)
        assertEquals(2, channel.items.size)
    }

    @Test
    fun parse_rssFeed_firstItemFields() = runBlocking {
        val item = RssParser().parse(sampleRssFeed).items[0]

        assertEquals("First Article", item.title)
        assertEquals("https://example.com/article-1", item.link)
        assertEquals("First article description", item.description)
        assertEquals("<p>Full content here</p>", item.content)
        assertEquals("guid-1", item.guid)
        assertEquals("Author One", item.author)
        assertEquals("Tue, 10 Sep 2024 12:00:00 GMT", item.pubDate)
        assertEquals(listOf("Tech", "News"), item.categories)
    }

    @Test
    fun parse_rssFeed_secondItemThumbnail() = runBlocking {
        val item = RssParser().parse(sampleRssFeed).items[1]

        assertEquals("Second Article", item.title)
        assertEquals("https://example.com/img2.jpg", item.image)
    }

    @Test
    fun parse_rssFeed_throwsOnInvalidXml() {
        assertFailsWith<IllegalArgumentException> {
            runBlocking { RssParser().parse("<not-a-feed><item>nope</item></not-a-feed>") }
        }
    }

    // ---------- RssParser (Atom 1.0) ----------

    private val sampleAtomFeed = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
            <title>Atom Feed Title</title>
            <link href="https://example.com/atom" rel="self" />
            <link href="https://example.com" />
            <subtitle>Atom feed subtitle</subtitle>
            <updated>2024-09-10T12:00:00Z</updated>
            <icon>https://example.com/icon.png</icon>
            <entry>
                <title>Atom Entry One</title>
                <link href="https://example.com/entry-1" />
                <id>atom-guid-1</id>
                <updated>2024-09-10T12:00:00Z</updated>
                <published>2024-09-10T10:00:00Z</published>
                <summary>Entry one summary</summary>
                <content type="html">&lt;p&gt;Entry one content&lt;/p&gt;</content>
                <author><name>Atom Author</name></author>
                <category term="Programming" />
            </entry>
        </feed>
    """.trimIndent()

    @Test
    fun parse_atomFeed_channelFields() = runBlocking {
        val channel = RssParser().parse(sampleAtomFeed)

        assertEquals("Atom Feed Title", channel.title)
        assertEquals("Atom feed subtitle", channel.description)
        assertEquals("2024-09-10T12:00:00Z", channel.lastBuildDate)
        assertNotNull(channel.image)
        assertEquals("https://example.com/icon.png", channel.image!!.url)
    }

    @Test
    fun parse_atomFeed_selfLinkSkipped() = runBlocking {
        val channel = RssParser().parse(sampleAtomFeed)
        // The non-self link should be the channel link, not the self link
        assertEquals("https://example.com", channel.link)
    }

    @Test
    fun parse_atomFeed_entryFields() = runBlocking {
        val channel = RssParser().parse(sampleAtomFeed)
        assertEquals(1, channel.items.size)

        val item = channel.items[0]
        assertEquals("Atom Entry One", item.title)
        assertEquals("https://example.com/entry-1", item.link)
        assertEquals("atom-guid-1", item.guid)
        assertEquals("Entry one summary", item.description)
        assertEquals("Atom Author", item.author)
        // <updated> is processed before <published>, both use pubDateIfNull,
        // so pubDate gets the updated value
        assertEquals("2024-09-10T12:00:00Z", item.pubDate)
    }

    @Test
    fun parse_atomFeed_entryCategory() = runBlocking {
        val channel = RssParser().parse(sampleAtomFeed)
        // Category with term attribute but empty text → uses term value
        assertEquals(1, channel.items[0].categories.size)
        assertEquals("Programming", channel.items[0].categories[0])
    }

    @Test
    fun parse_atomFeed_entryContent() = runBlocking {
        val channel = RssParser().parse(sampleAtomFeed)
        assertNotNull(channel.items[0].content)
        assertContains(channel.items[0].content!!, "Entry one content")
    }
}
