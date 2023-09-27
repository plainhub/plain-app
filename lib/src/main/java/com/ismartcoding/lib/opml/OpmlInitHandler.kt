package com.ismartcoding.lib.opml

import org.xmlpull.v1.XmlPullParser

internal class OpmlInitHandler : OpmlSectionHandler<String> {
    private var started = false
    private var version: String = ""

    override fun startTag(xpp: XmlPullParser) {
        ValidityCheck.require(xpp, XmlPullParser.START_TAG, "opml")
        version = xpp.getAttributeValue(null, "version") ?: ""
        if (version.isEmpty()) {
            throw OpmlParseException("opml element does not have required attribute version")
        }
        started = true
    }

    override fun text(xpp: XmlPullParser) {
        ValidityCheck.requireNoText(xpp, "opml", started)
    }

    override fun endTag(xpp: XmlPullParser) {
        started = false
    }

    override fun get(): String {
        return version
    }
}
