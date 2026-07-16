package com.ismartcoding.plain.lib.html2md

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for [HtmlParser] — the pure-Kotlin HTML parser that backs both
 * `MDConverter` and the feed `HtmlUtils` sanitizer (which replaced jsoup).
 *
 * Coverage focuses on the parsing paths that real-world RSS/Atom HTML content
 * exercises: nested elements, void elements, attributes (quoted/unquoted),
 * entities, CDATA sections, HTML comments, malformed tags, and DOM queries.
 */
class HtmlParserTest {

    private fun parse(html: String): HtmlElement = HtmlParser.parse(html)

    @Test
    fun emptyStringProducesEmptyRoot() {
        val root = parse("")
        assertEquals(0, root.childNodeSize())
    }

    @Test
    fun plainTextBecomesTextNode() {
        val root = parse("hello world")
        assertEquals(1, root.childNodeSize())
        val child = root.childNode(0)
        assertTrue(child is HtmlTextNode)
        assertEquals("hello world", (child as HtmlTextNode).text())
    }

    @Test
    fun singleElement() {
        val root = parse("<p>hi</p>")
        val p = root.childNode(0) as HtmlElement
        assertEquals("p", p.tagName())
        assertEquals("hi", p.wholeText())
    }

    @Test
    fun nestedElements() {
        val root = parse("<div><p>hello</p></div>")
        val div = root.childNode(0) as HtmlElement
        assertEquals("div", div.tagName())
        val p = div.childNode(0) as HtmlElement
        assertEquals("p", p.tagName())
        assertEquals("hello", p.wholeText())
    }

    @Test
    fun voidElementDoesNotPushStack() {
        val root = parse("<p>before<br>after</p>")
        val p = root.childNode(0) as HtmlElement
        // p should have three children: text "before", <br>, text "after"
        assertEquals(3, p.childNodeSize())
        assertTrue(p.childNode(1) is HtmlElement)
        assertEquals("br", (p.childNode(1) as HtmlElement).tagName())
    }

    @Test
    fun selfClosingTagDoesNotPushStack() {
        val root = parse("<div><img src=\"x.png\"/></div>")
        val div = root.childNode(0) as HtmlElement
        assertEquals(1, div.childNodeSize())
        val img = div.childNode(0) as HtmlElement
        assertEquals("img", img.tagName())
        assertEquals("x.png", img.attr("src"))
    }

    @Test
    fun attributesDoubleQuoted() {
        val root = parse("<a href=\"https://example.com\" title=\"hi\">link</a>")
        val a = root.childNode(0) as HtmlElement
        assertEquals("https://example.com", a.attr("href"))
        assertEquals("hi", a.attr("title"))
    }

    @Test
    fun attributesSingleQuoted() {
        val root = parse("<a href='https://example.com'>link</a>")
        val a = root.childNode(0) as HtmlElement
        assertEquals("https://example.com", a.attr("href"))
    }

    @Test
    fun attributesUnquoted() {
        val root = parse("<img src=pic.png>")
        val img = root.childNode(0) as HtmlElement
        assertEquals("pic.png", img.attr("src"))
    }

    @Test
    fun booleanAttribute() {
        val root = parse("<input disabled>")
        val input = root.childNode(0) as HtmlElement
        assertTrue(input.hasAttr("disabled"))
        assertEquals("", input.attr("disabled"))
    }

    @Test
    fun namedEntitiesDecoded() {
        val root = parse("<p>&amp;&lt;&gt;&quot;&nbsp;</p>")
        val p = root.childNode(0) as HtmlElement
        // &nbsp; decodes to a single U+00A0 (non-breaking space), not U+0020.
        assertEquals("&<>\"\u00A0", p.wholeText())
    }

    @Test
    fun numericEntityDecoded() {
        val root = parse("<p>&#65;&#x42;</p>")
        val p = root.childNode(0) as HtmlElement
        assertEquals("AB", p.wholeText())
    }

    @Test
    fun cdataPreservedAsText() {
        val root = parse("<p><![CDATA[<b>raw</b>]]></p>")
        val p = root.childNode(0) as HtmlElement
        assertEquals("<b>raw</b>", p.wholeText())
    }

    @Test
    fun htmlCommentSkipped() {
        val root = parse("<p>a<!-- comment -->b</p>")
        val p = root.childNode(0) as HtmlElement
        assertEquals("ab", p.wholeText())
    }

    @Test
    fun unclosedTagRecovers() {
        val root = parse("<p>hello<p>world")
        // The parser is permissive but does NOT implement HTML5 auto-closing
        // rules: the second <p> becomes a CHILD of the first <p> rather than
        // a sibling. Document this so a future HTML5-compliant rewrite is a
        // conscious decision.
        assertEquals(1, root.childNodeSize())
        val outerP = root.childNode(0) as HtmlElement
        assertEquals("p", outerP.tagName())
        // The first <p> contains the text "hello" followed by the nested <p>.
        assertEquals(2, outerP.childNodeSize())
    }

    @Test
    fun mismatchedCloseTagIgnored() {
        // </div> with no matching open tag — should not crash, just be ignored
        val root = parse("<p>hi</div></p>")
        val p = root.childNode(0) as HtmlElement
        assertEquals("hi", p.wholeText())
    }

    @Test
    fun getElementById() {
        val root = parse("<div id=\"main\">content</div>")
        val found = root.getElementById("main")
        assertNotNull(found)
        assertEquals("main", found.attr("id"))
    }

    @Test
    fun getElementByIdReturnsNullIfMissing() {
        val root = parse("<div>content</div>")
        assertNull(root.getElementById("missing"))
    }

    @Test
    fun getElementsByTag() {
        val root = parse("<ul><li>a</li><li>b</li></ul>")
        val items = root.getElementsByTag("li")
        assertEquals(2, items.size)
        assertEquals("a", items[0].wholeText())
        assertEquals("b", items[1].wholeText())
    }

    @Test
    fun selectByTag() {
        val root = parse("<div><p>1</p><p>2</p></div>")
        val ps = root.select("p")
        assertEquals(2, ps.size)
    }

    @Test
    fun selectById() {
        val root = parse("<div id=\"a\">1</div><div id=\"b\">2</div>")
        val found = root.select("#b")
        assertEquals(1, found.size)
        assertEquals("2", found[0].wholeText())
    }

    @Test
    fun selectByAttributeExists() {
        val root = parse("<img src=\"a.png\"><img src=\"b.png\" alt=\"B\">")
        val withAlt = root.select("img[alt]")
        assertEquals(1, withAlt.size)
        assertEquals("B", withAlt[0].attr("alt"))
    }

    @Test
    fun selectByAttributeEquals() {
        val root = parse("<a href=\"x\">1</a><a href=\"y\">2</a>")
        val found = root.select("a[href=\"y\"]")
        assertEquals(1, found.size)
        assertEquals("2", found[0].wholeText())
    }

    @Test
    fun selectByAttributeStartsWith() {
        val root = parse("<a href=\"https://a.com\">1</a><a href=\"http://b.com\">2</a>")
        val found = root.select("a[href^=\"https://\"]")
        assertEquals(1, found.size)
        assertEquals("1", found[0].wholeText())
    }

    @Test
    fun selectFirstReturnsNullIfNoMatch() {
        val root = parse("<div>content</div>")
        assertNull(root.selectFirst("p"))
    }

    @Test
    fun outerHtmlRoundTrip() {
        val html = "<p>hello <b>world</b></p>"
        val root = parse(html)
        assertEquals(html, root.childNode(0).outerHtml())
    }

    @Test
    fun textNodeOuterHtmlNotEscaped() {
        // The shared HtmlTextNode deliberately does not escape — feed
        // HtmlUtils does its own escaping on serialization. Document this
        // so any future refactor doesn't silently break feed sanitization.
        val root = parse("<p>&lt;script&gt;</p>")
        val p = root.childNode(0) as HtmlElement
        val text = p.childNode(0) as HtmlTextNode
        assertEquals("<script>", text.outerHtml())
    }

    @Test
    fun attributesPreserveOrderAndCaseInsensitiveLookup() {
        val root = parse("<input ID=\"x\" Name=\"y\">")
        val input = root.childNode(0) as HtmlElement
        // Attribute names are stored lowercased by the parser.
        assertEquals("x", input.attr("id"))
        assertEquals("y", input.attr("name"))
    }
}
