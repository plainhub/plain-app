package com.ismartcoding.plain.lib.opml

internal class OpmlInitHandler : OpmlSectionHandler<String> {
    private var started = false
    private var version: String = ""

    override fun startTag(xpp: SimpleXmlReader) {
        ValidityCheck.require(xpp, SimpleXmlReader.START_TAG, "opml")
        version = xpp.getAttributeValue(null, "version") ?: ""
        if (version.isEmpty()) {
            throw OpmlParseException("opml element does not have required attribute version")
        }
        started = true
    }

    override fun text(xpp: SimpleXmlReader) {
        ValidityCheck.requireNoText(xpp, "opml", started)
    }

    override fun endTag(xpp: SimpleXmlReader) {
        started = false
    }

    override fun get(): String {
        return version
    }
}
