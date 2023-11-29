package com.ismartcoding.lib.rss.internal

import org.xmlpull.v1.XmlPullParser

internal fun XmlPullParser.nextTrimmedText(): String? = this.nextText()?.trim()

internal fun XmlPullParser.contains(key: String): Boolean {
    return this.name.equals(key, ignoreCase = true)
}

internal fun XmlPullParser.attributeValue(key: String): String? {
    return this.getAttributeValue(null, key)?.trim()
}
