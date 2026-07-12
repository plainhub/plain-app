package com.ismartcoding.plain.lib.xml

import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.serializer

class XmlDecodeException(message: String, cause: Throwable? = null) : Exception(message, cause)

inline fun <reified T> decodeXml(xml: String): T {
    val root = XmlTreeParser.parse(xml)
    val serializer = serializer<T>()
    val json = defaultJson()
    val jsonElement = xmlElementToJson(root, serializer.descriptor)
    return json.decodeFromJsonElement(serializer, jsonElement)
}

/**
 * Parse a SOAP envelope XML and decode the first child of `<Body>` into type [T].
 *
 * Equivalent to the old `XmlHelper.parseData`: extract `<Body>`'s first child,
 * then treat that child as the root wrapper whose children map to [T]'s properties.
 */
inline fun <reified T> parseData(xml: String): T {
    val root = XmlTreeParser.parse(xml)
    val body = root.children.firstOrNull { it.localName.endsWith("Body") }
        ?: throw XmlDecodeException("No Body element found in XML")
    val firstChild = body.children.firstOrNull()
        ?: throw XmlDecodeException("Body element has no child")
    val serializer = serializer<T>()
    val json = defaultJson()
    val jsonElement = xmlElementToJson(firstChild, serializer.descriptor)
    return json.decodeFromJsonElement(serializer, jsonElement)
}

@PublishedApi
internal fun defaultJson() = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

// --- Internal XML tree ---

@PublishedApi
internal class XmlElement(
    val name: String,
    val attributes: Map<String, String>,
    val children: List<XmlElement>,
    val text: String,
) {
    /** Tag name with namespace prefix stripped (e.g. `u:Body` -> `Body`). */
    val localName: String get() = name.substringAfter(':')
}

@PublishedApi
internal object XmlTreeParser {
    fun parse(xml: String): XmlElement {
        val parser = Parser(xml)
        parser.skipProlog()
        parser.skipWhitespace()
        if (!parser.hasMore() || parser.peek() != '<') {
            throw XmlDecodeException("Expected '<' at start of XML")
        }
        return parser.parseElement()
    }

    private class Parser(private val xml: String) {
        private var pos = 0

        fun peek(): Char = xml[pos]

        fun hasMore(): Boolean = pos < xml.length

        fun skipWhitespace() {
            while (pos < xml.length && xml[pos].isWhitespace()) pos++
        }

        fun skipProlog() {
            skipWhitespace()
            if (pos < xml.length && xml.startsWith("<?xml", pos)) {
                val end = xml.indexOf("?>", pos)
                pos = if (end < 0) xml.length else end + 2
            }
        }

        fun parseElement(): XmlElement {
            expect('<')
            val name = parseName()
            val attrs = linkedMapOf<String, String>()
            while (true) {
                skipWhitespace()
                if (!hasMore()) throw XmlDecodeException("Unexpected end of XML in <$name>")
                when (peek()) {
                    '>' -> { pos++; break }
                    '/' -> { pos++; expect('>'); return XmlElement(name, attrs, emptyList(), "") }
                    else -> {
                        val attrName = parseName()
                        skipWhitespace()
                        expect('=')
                        skipWhitespace()
                        val quote = peek()
                        if (quote != '"' && quote != '\'') {
                            throw XmlDecodeException("Expected quote in attribute value of <$name>")
                        }
                        pos++
                        val valueEnd = xml.indexOf(quote, pos)
                        if (valueEnd < 0) throw XmlDecodeException("Unclosed attribute value in <$name>")
                        attrs[attrName] = decodeEntities(xml.substring(pos, valueEnd))
                        pos = valueEnd + 1
                    }
                }
            }

            val children = mutableListOf<XmlElement>()
            val textBuilder = StringBuilder()
            while (true) {
                if (!hasMore()) throw XmlDecodeException("Unexpected end of XML, missing </$name>")
                if (xml.startsWith("</", pos)) {
                    pos += 2
                    parseName() // end tag name; ignore mismatch for leniency
                    skipWhitespace()
                    expect('>')
                    break
                }
                when {
                    xml.startsWith("<!--", pos) -> skipUntil("-->")
                    xml.startsWith("<![CDATA[", pos) -> {
                        pos += 9
                        val end = xml.indexOf("]]>", pos)
                        if (end < 0) throw XmlDecodeException("Unclosed CDATA section")
                        textBuilder.append(xml.substring(pos, end))
                        pos = end + 3
                    }
                    xml.startsWith("<?", pos) -> skipUntil("?>")
                    xml.startsWith("<!", pos) -> skipUntil(">")
                    peek() == '<' -> children.add(parseElement())
                    else -> {
                        val textEnd = xml.indexOf('<', pos)
                        val end = if (textEnd < 0) xml.length else textEnd
                        textBuilder.append(decodeEntities(xml.substring(pos, end)))
                        pos = end
                    }
                }
            }
            return XmlElement(name, attrs, children, textBuilder.toString().trim())
        }

        private fun parseName(): String {
            val start = pos
            while (pos < xml.length) {
                val c = xml[pos]
                if (c.isWhitespace() || c == '>' || c == '/' || c == '=' || c == '<') break
                pos++
            }
            return xml.substring(start, pos)
        }

        private fun expect(c: Char) {
            if (!hasMore() || peek() != c) {
                throw XmlDecodeException("Expected '$c' at position $pos")
            }
            pos++
        }

        private fun skipUntil(marker: String) {
            val end = xml.indexOf(marker, pos)
            pos = if (end < 0) xml.length else end + marker.length
        }
    }
}

// --- XML to JSON conversion (descriptor-driven) ---

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@PublishedApi
internal fun xmlElementToJson(element: XmlElement, descriptor: SerialDescriptor): JsonElement {
    return when (descriptor.kind) {
        StructureKind.LIST -> {
            // Element's direct children are list items (gson sameNameList=false semantics
            // where child tag names are ignored when the target type is a List).
            JsonArray(element.children.map { xmlElementToJson(it, descriptor.getElementDescriptor(0)) })
        }
        StructureKind.CLASS, StructureKind.OBJECT, PolymorphicKind.OPEN -> {
            val map = linkedMapOf<String, JsonElement>()
            for (i in 0 until descriptor.elementsCount) {
                val fieldName = descriptor.getElementName(i)
                val childDescriptor = descriptor.getElementDescriptor(i)
                val matching = element.children.filter { it.localName == fieldName }
                if (matching.isEmpty()) continue
                map[fieldName] = when (childDescriptor.kind) {
                    StructureKind.LIST -> {
                        val itemDescriptor = childDescriptor.getElementDescriptor(0)
                        if (matching.size == 1) {
                            val wrapper = matching.first()
                            when {
                                // Wrapper element: <serviceList><service>…</service></serviceList>
                                wrapper.children.isNotEmpty() -> JsonArray(wrapper.children.map { xmlElementToJson(it, itemDescriptor) })
                                // Single element with text: <items>value</items>
                                wrapper.text.isNotEmpty() -> JsonArray(listOf(xmlElementToJson(wrapper, itemDescriptor)))
                                // Empty wrapper: <serviceList></serviceList> → empty list
                                else -> JsonArray(emptyList())
                            }
                        } else {
                            // Same-named siblings: <item>1</item><item>2</item>
                            JsonArray(matching.map { xmlElementToJson(it, itemDescriptor) })
                        }
                    }
                    else -> xmlElementToJson(matching.first(), childDescriptor)
                }
            }
            JsonObject(map)
        }
        PrimitiveKind.INT -> JsonPrimitive(element.text.toIntOrNull() ?: 0)
        PrimitiveKind.LONG -> JsonPrimitive(element.text.toLongOrNull() ?: 0L)
        PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> JsonPrimitive(element.text.toDoubleOrNull() ?: 0.0)
        PrimitiveKind.BOOLEAN -> JsonPrimitive(element.text.toBooleanStrictOrNull() ?: false)
        else -> JsonPrimitive(element.text)
    }
}

private fun decodeEntities(s: String): String {
    if ('&' !in s) return s
    return s.replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
}
