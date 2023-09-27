package com.ismartcoding.lib.opml

import com.ismartcoding.lib.opml.entity.Head
import org.xmlpull.v1.XmlPullParser
import java.util.ArrayDeque
import java.util.Deque

internal class OpmlHeadHandler : OpmlSectionHandler<Head> {
    private val stack: Deque<String> = ArrayDeque()
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

    @Throws(OpmlParseException::class)
    override fun startTag(xpp: XmlPullParser) {
        // no nested elements in head & we do not push <head> itself on the stack
        if (!stack.isEmpty()) {
            throw OpmlParseException(String.format("head section contains nested element %s inside element %s", xpp.name, stack.peek()))
        }
        stack.push(xpp.name)
        started = true
    }

    @Throws(OpmlParseException::class)
    override fun text(xpp: XmlPullParser) {
        if (stack.isEmpty()) {
            // we should be between <head> and first element
            ValidityCheck.requireNoText(xpp, "head", started)
            return
        }
        val text = xpp.text
        when (stack.peek()) {
            "title" -> title = text
            "dateCreated" -> dateCreated = text
            "dateModified" -> dateModified = text
            "ownerName" -> ownerName = text
            "ownerEmail" -> ownerEmail = text
            "ownerId" -> ownerId = text
            "docs" -> docs = text
            "expansionState" -> {
                val split = text.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (part in split) {
                    val newPart = part.trim { it <= ' ' }
                    if (newPart.isNotEmpty()) {
                        try {
                            expansionState.add(newPart.trim { it <= ' ' }.toInt())
                        } catch (e: NumberFormatException) {
                            throw OpmlParseException("expansionState must be a comma-separated list of line numbers")
                        }
                    }
                }
            }

            "vertScrollState" -> {
                val trimmed = text.trim { it <= ' ' }
                vertScrollState =
                    if (trimmed.isNotEmpty()) {
                        try {
                            trimmed.trim { it <= ' ' }.toInt()
                        } catch (e: NumberFormatException) {
                            throw OpmlParseException("vertScrollState must be a number")
                        }
                    } else {
                        null
                    }
            }

            "windowBottom" -> {
                val trimmed = text.trim { it <= ' ' }
                windowBottom =
                    if (trimmed.isNotEmpty()) {
                        try {
                            text.trim { it <= ' ' }.toInt()
                        } catch (e: NumberFormatException) {
                            throw OpmlParseException("windowBottom must be a number")
                        }
                    } else {
                        null
                    }
            }

            "windowLeft" -> {
                val trimmed = text.trim { it <= ' ' }
                windowLeft =
                    if (trimmed.isNotEmpty()) {
                        try {
                            text.trim { it <= ' ' }.toInt()
                        } catch (e: NumberFormatException) {
                            throw OpmlParseException("windowLeft must be a number")
                        }
                    } else {
                        null
                    }
            }

            "windowRight" -> {
                val trimmed = text.trim { it <= ' ' }
                windowRight =
                    if (trimmed.isNotEmpty()) {
                        try {
                            text.trim { it <= ' ' }.toInt()
                        } catch (e: NumberFormatException) {
                            throw OpmlParseException("windowRight must be a number")
                        }
                    } else {
                        null
                    }
            }

            "windowTop" -> {
                val trimmed = text.trim { it <= ' ' }
                windowTop =
                    if (trimmed.isNotEmpty()) {
                        try {
                            text.trim { it <= ' ' }.toInt()
                        } catch (e: NumberFormatException) {
                            throw OpmlParseException("windowTop must be a number")
                        }
                    } else {
                        null
                    }
            }
        }
    }

    @Throws(OpmlParseException::class)
    override fun endTag(xpp: XmlPullParser) {
        stack.poll()
        started = false
    }

    override fun get(): Head {
        return Head(
            title, dateCreated, dateModified, ownerName, ownerEmail, ownerId, docs, expansionState,
            vertScrollState, windowTop, windowLeft, windowBottom, windowRight,
        )
    }
}
