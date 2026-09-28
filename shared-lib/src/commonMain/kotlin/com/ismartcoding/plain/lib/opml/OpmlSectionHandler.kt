package com.ismartcoding.plain.lib.opml

import com.ismartcoding.plain.lib.xml.SimpleXmlReader

internal interface OpmlSectionHandler<E> {
    fun startTag(xpp: SimpleXmlReader)

    fun text(xpp: SimpleXmlReader)

    fun endTag(xpp: SimpleXmlReader)

    fun get(): E
}
