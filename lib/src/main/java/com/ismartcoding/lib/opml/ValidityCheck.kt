package com.ismartcoding.lib.opml

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

object ValidityCheck {
    fun require(
        xpp: XmlPullParser,
        position: Int,
        name: String,
    ) {
        requirePosition(xpp, position)
        requireName(xpp, name)
    }

    fun requirePosition(
        xpp: XmlPullParser,
        position: Int,
    ) {
        try {
            if (xpp.eventType != position) {
                throw OpmlParseException(
                    String.format("required position %s but found position %s", translate(position), translate(xpp.eventType)),
                )
            }
        } catch (e: XmlPullParserException) {
            throw OpmlParseException(e)
        }
    }

    fun requireName(
        xpp: XmlPullParser,
        name: String,
    ) {
        if (xpp.name != name) {
            throw OpmlParseException(String.format("required element <%s> but found <%s>", name, xpp.name))
        }
    }

    fun requireNoText(
        xpp: XmlPullParser,
        elementName: String,
        insideElement: Boolean,
    ) {
        if (xpp.text.isNotBlank()) {
            if (insideElement) {
                throw OpmlParseException(String.format("text inside element <%s>: \"%s\"", elementName, xpp.text))
            } else {
                throw OpmlParseException(String.format("required element <%s> but found text: \"%s\"", elementName, xpp.text))
            }
        }
    }

    fun translate(position: Int): String {
        when (position) {
            XmlPullParser.START_DOCUMENT -> return "START_DOCUMENT"
            XmlPullParser.START_TAG -> return "START_TAG"
            XmlPullParser.TEXT -> return "TEXT"
            XmlPullParser.END_TAG -> return "END_TAG"
            XmlPullParser.END_DOCUMENT -> return "END_DOCUMENT"
        }
        return position.toString()
    }
}
