package com.ismartcoding.plain.lib.opml

internal interface OpmlSectionHandler<E> {
    fun startTag(xpp: SimpleXmlReader)

    fun text(xpp: SimpleXmlReader)

    fun endTag(xpp: SimpleXmlReader)

    fun get(): E
}
