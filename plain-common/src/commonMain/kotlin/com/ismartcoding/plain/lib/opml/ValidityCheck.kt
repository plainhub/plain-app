package com.ismartcoding.plain.lib.opml

internal object ValidityCheck {
    fun require(
        xpp: SimpleXmlReader,
        position: Int,
        name: String,
    ) {
        requirePosition(xpp, position)
        requireName(xpp, name)
    }

    fun requirePosition(
        xpp: SimpleXmlReader,
        position: Int,
    ) {
        if (xpp.eventType != position) {
            throw OpmlParseException(
                "required position ${translate(position)} but found position ${translate(xpp.eventType)}",
            )
        }
    }

    fun requireName(
        xpp: SimpleXmlReader,
        name: String,
    ) {
        if (xpp.name != name) {
            throw OpmlParseException("required element <$name> but found <${xpp.name}>")
        }
    }

    fun requireNoText(
        xpp: SimpleXmlReader,
        elementName: String,
        insideElement: Boolean,
    ) {
        if (xpp.text.isNotBlank()) {
            if (insideElement) {
                throw OpmlParseException("text inside element <$elementName>: \"${xpp.text}\"")
            } else {
                throw OpmlParseException("required element <$elementName> but found text: \"${xpp.text}\"")
            }
        }
    }

    fun translate(position: Int): String {
        return when (position) {
            SimpleXmlReader.START_DOCUMENT -> "START_DOCUMENT"
            SimpleXmlReader.START_TAG -> "START_TAG"
            SimpleXmlReader.TEXT -> "TEXT"
            SimpleXmlReader.END_TAG -> "END_TAG"
            SimpleXmlReader.END_DOCUMENT -> "END_DOCUMENT"
            else -> position.toString()
        }
    }
}
