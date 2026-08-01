package com.ismartcoding.plain.lib.opml

/**
 * Minimal pure-Kotlin event-based XML reader.
 * Provides a subset of XmlPullParser interface sufficient for OPML parsing.
 */
internal class SimpleXmlReader(private val xml: String) {

    companion object {
        const val START_DOCUMENT = 0
        const val START_TAG = 1
        const val TEXT = 2
        const val END_TAG = 3
        const val END_DOCUMENT = 4
    }

    private var pos = 0
    private var pendingEndTag = false

    var eventType: Int = START_DOCUMENT
        private set
    var name: String = ""
        private set
    var text: String = ""
        private set
    private var attributes: List<Pair<String, String>> = emptyList()

    val attributeCount: Int get() = attributes.size

    fun getAttributeName(index: Int): String = attributes[index].first
    fun getAttributeValue(index: Int): String = attributes[index].second
    fun getAttributeValue(ns: String?, name: String): String? =
        attributes.firstOrNull { it.first == name }?.second

    fun next(): Int {
        if (eventType == END_DOCUMENT) return END_DOCUMENT

        if (pendingEndTag) {
            pendingEndTag = false
            eventType = END_TAG
            return eventType
        }

        if (pos >= xml.length) {
            eventType = END_DOCUMENT
            return eventType
        }

        // Skip comments, processing instructions, doctypes
        while (pos < xml.length) {
            when {
                xml.startsWith("<!--", pos) -> skipUntil("-->")
                xml.startsWith("<![CDATA[", pos) -> return parseCData()
                xml.startsWith("<?", pos) -> skipUntil("?>")
                xml.startsWith("<!", pos) -> skipUntil(">")
                else -> break
            }
        }

        if (pos >= xml.length) {
            eventType = END_DOCUMENT
            return eventType
        }

        return when {
            xml.startsWith("</", pos) -> parseEndTag()
            xml[pos] == '<' -> parseStartTag()
            else -> parseText()
        }
    }

    private fun parseStartTag(): Int {
        pos++ // skip '<'
        val nameEnd = findNameEnd(pos)
        name = xml.substring(pos, nameEnd)
        pos = nameEnd

        val attrs = mutableListOf<Pair<String, String>>()

        while (pos < xml.length) {
            skipWhitespace()
            if (pos >= xml.length || xml[pos] == '>' || xml[pos] == '/') break

            val attrNameStart = pos
            val attrNameEnd = findChar(pos) { it == '=' || it.isWhitespace() || it == '>' || it == '/' }
            if (attrNameEnd == attrNameStart) {
                pos++
                continue
            }
            val attrName = xml.substring(attrNameStart, attrNameEnd)
            pos = attrNameEnd

            skipWhitespace()
            if (pos < xml.length && xml[pos] == '=') {
                pos++
                skipWhitespace()
                if (pos < xml.length && (xml[pos] == '"' || xml[pos] == '\'')) {
                    val quote = xml[pos++]
                    val valueEnd = xml.indexOf(quote, pos)
                    if (valueEnd < 0) throw OpmlParseException("Unclosed attribute value in <$name>")
                    attrs.add(attrName to decodeEntities(xml.substring(pos, valueEnd)))
                    pos = valueEnd + 1
                }
            }
        }

        attributes = attrs
        text = "" // Clear text on START_TAG (matches XmlPullParser behavior)

        if (pos < xml.length && xml[pos] == '/') {
            pos++
            pendingEndTag = true
        }
        if (pos < xml.length && xml[pos] == '>') {
            pos++
        }

        eventType = START_TAG
        return eventType
    }

    private fun parseEndTag(): Int {
        pos += 2 // skip '</'
        val nameEnd = findChar(pos) { it == '>' }
        name = xml.substring(pos, nameEnd).trim()
        pos = nameEnd + 1 // skip '>'
        text = "" // Clear text on END_TAG (matches XmlPullParser behavior)

        eventType = END_TAG
        return eventType
    }

    private fun parseText(): Int {
        val textEnd = xml.indexOf('<', pos)
        val end = if (textEnd < 0) xml.length else textEnd
        text = decodeEntities(xml.substring(pos, end))
        pos = end

        name = "" // Clear name on TEXT events (matches XmlPullParser behavior)
        eventType = TEXT
        return eventType
    }

    private fun parseCData(): Int {
        pos += 9 // skip "<![CDATA["
        val end = xml.indexOf("]]>", pos)
        text = if (end < 0) {
            xml.substring(pos).also { pos = xml.length }
        } else {
            xml.substring(pos, end).also { pos = end + 3 }
        }
        name = ""
        eventType = TEXT
        return eventType
    }

    private fun skipUntil(marker: String) {
        val end = xml.indexOf(marker, pos)
        pos = if (end < 0) xml.length else end + marker.length
    }

    private fun skipWhitespace() {
        while (pos < xml.length && xml[pos].isWhitespace()) pos++
    }

    private fun findNameEnd(start: Int): Int =
        findChar(start) { it.isWhitespace() || it == '>' || it == '/' }

    private fun findChar(start: Int, predicate: (Char) -> Boolean): Int {
        var i = start
        while (i < xml.length && !predicate(xml[i])) i++
        return i
    }

    private fun decodeEntities(s: String): String {
        if ('&' !in s) return s
        return s.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }
}
