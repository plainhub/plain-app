package com.ismartcoding.plain.lib.opml

import org.xmlpull.v1.XmlPullParser

internal interface OpmlSectionHandler<E> {
    fun startTag(xpp: XmlPullParser)

    fun text(xpp: XmlPullParser)

    fun endTag(xpp: XmlPullParser)

    fun get(): E
}
