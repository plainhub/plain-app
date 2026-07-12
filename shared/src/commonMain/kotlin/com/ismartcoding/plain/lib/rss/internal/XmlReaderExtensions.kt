package com.ismartcoding.plain.lib.rss.internal

import com.ismartcoding.plain.lib.opml.SimpleXmlReader

/**
 * Extension functions for SimpleXmlReader, matching the XmlPullParser API
 * used by the original RSS parser.
 */

internal fun SimpleXmlReader.nextTrimmedText(): String? = nextText()?.trim()

internal fun SimpleXmlReader.contains(key: String): Boolean = name.equals(key, ignoreCase = true)

internal fun SimpleXmlReader.attributeValue(key: String): String? =
    getAttributeValue(null, key)?.trim()

internal fun SimpleXmlReader.nextText(): String {
    val event = next()
    if (event == SimpleXmlReader.TEXT) {
        val result = text
        next() // consume END_TAG
        return result
    }
    return ""
}
