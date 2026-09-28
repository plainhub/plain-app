package com.ismartcoding.plain.lib.opml

import com.ismartcoding.plain.lib.opml.entity.Head

internal class OpmlHeadHandler : OpmlSectionHandler<Head> {
    private val stack = ArrayDeque<String>()
    private var started = false
    private var title: String = ""
    private var dateCreated: String = ""
    private var dateModified: String = ""
    private var ownerName: String = ""
    private var ownerEmail: String = ""
    private var ownerId: String = ""
    private var docs: String = ""
    private val expansionState: MutableList<Int> = ArrayList()
    private var vertScrollState: Int? = null
    private var windowTop: Int? = null
    private var windowLeft: Int? = null
    private var windowBottom: Int? = null
    private var windowRight: Int? = null

    override fun startTag(xpp: SimpleXmlReader) {
        if (stack.isNotEmpty()) {
            throw OpmlParseException("head section contains nested element ${xpp.name} inside element ${stack.last()}")
        }
        stack.addLast(xpp.name)
        started = true
    }

    override fun text(xpp: SimpleXmlReader) {
        if (stack.isEmpty()) {
            ValidityCheck.requireNoText(xpp, "head", started)
            return
        }
        val text = xpp.text
        when (stack.last()) {
            "title" -> title = text
            "dateCreated" -> dateCreated = text
            "dateModified" -> dateModified = text
            "ownerName" -> ownerName = text
            "ownerEmail" -> ownerEmail = text
            "ownerId" -> ownerId = text
            "docs" -> docs = text
            "expansionState" -> {
                for (part in text.split(",")) {
                    val trimmed = part.trim()
                    if (trimmed.isNotEmpty()) {
                        try {
                            expansionState.add(trimmed.toInt())
                        } catch (e: NumberFormatException) {
                            throw OpmlParseException("expansionState must be a comma-separated list of line numbers")
                        }
                    }
                }
            }

            "vertScrollState" -> vertScrollState = parseIntOrNull(text, "vertScrollState")
            "windowBottom" -> windowBottom = parseIntOrNull(text, "windowBottom")
            "windowLeft" -> windowLeft = parseIntOrNull(text, "windowLeft")
            "windowRight" -> windowRight = parseIntOrNull(text, "windowRight")
            "windowTop" -> windowTop = parseIntOrNull(text, "windowTop")
        }
    }

    private fun parseIntOrNull(text: String, fieldName: String): Int? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return try {
            trimmed.toInt()
        } catch (e: NumberFormatException) {
            throw OpmlParseException("$fieldName must be a number")
        }
    }

    override fun endTag(xpp: SimpleXmlReader) {
        stack.removeLastOrNull()
        started = false
    }

    override fun get(): Head {
        return Head(
            title, dateCreated, dateModified, ownerName, ownerEmail, ownerId, docs, expansionState,
            vertScrollState, windowTop, windowLeft, windowBottom, windowRight,
        )
    }
}
