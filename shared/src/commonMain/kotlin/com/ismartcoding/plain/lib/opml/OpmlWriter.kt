package com.ismartcoding.plain.lib.opml

import com.ismartcoding.plain.lib.opml.entity.Body
import com.ismartcoding.plain.lib.opml.entity.Head
import com.ismartcoding.plain.lib.opml.entity.Opml
import com.ismartcoding.plain.lib.opml.entity.Outline

class OpmlWriter {
    fun write(opml: Opml): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.append(LINE_SEP)
        writeOpml(sb, opml)
        return sb.toString()
    }

    private fun writeOpml(sb: StringBuilder, opml: Opml) {
        sb.append("<opml version=\"").append(escapeAttr(opml.version)).append("\">")
        sb.append(LINE_SEP)
        writeHead(sb, opml.head)
        writeBody(sb, opml.body)
        sb.append("</opml>")
        sb.append(LINE_SEP)
    }

    private fun writeHead(sb: StringBuilder, head: Head) {
        sb.append(SINGLE_INDENT).append("<head>").append(LINE_SEP)
        writeHeadElement(sb, "title", head.title)
        writeHeadElement(sb, "dateCreated", head.dateCreated)
        writeHeadElement(sb, "dateModified", head.dateModified)
        writeHeadElement(sb, "ownerName", head.ownerName)
        writeHeadElement(sb, "ownerEmail", head.ownerEmail)
        writeHeadElement(sb, "ownerId", head.ownerId)
        writeHeadElement(sb, "docs", head.docs)
        if (head.expansionState.isNotEmpty()) {
            writeHeadElement(sb, "expansionState", head.expansionStateString)
        }
        writeHeadIntElement(sb, "vertScrollState", head.vertScrollState)
        writeHeadIntElement(sb, "windowTop", head.windowTop)
        writeHeadIntElement(sb, "windowLeft", head.windowLeft)
        writeHeadIntElement(sb, "windowBottom", head.windowBottom)
        writeHeadIntElement(sb, "windowRight", head.windowRight)
        sb.append(SINGLE_INDENT).append("</head>").append(LINE_SEP)
    }

    private fun writeHeadElement(sb: StringBuilder, name: String, value: String) {
        if (value.isBlank()) return
        sb.append(DOUBLE_INDENT)
            .append('<').append(name).append('>')
            .append(escapeText(value))
            .append("</").append(name).append('>')
            .append(LINE_SEP)
    }

    private fun writeHeadIntElement(sb: StringBuilder, name: String, value: Int?) {
        if (value == null) return
        writeHeadElement(sb, name, value.toString())
    }

    private fun writeBody(sb: StringBuilder, body: Body) {
        sb.append(SINGLE_INDENT).append("<body>").append(LINE_SEP)
        for (outline in body.outlines) {
            writeOutline(sb, outline)
        }
        sb.append(SINGLE_INDENT).append("</body>").append(LINE_SEP)
    }

    private fun writeOutline(sb: StringBuilder, outline: Outline) {
        sb.append(DOUBLE_INDENT).append("<outline")
        for ((key, value) in outline.attributes.entries) {
            sb.append(' ').append(key).append("=\"").append(escapeAttr(value)).append('"')
        }
        if (outline.subElements.isEmpty()) {
            sb.append("/>").append(LINE_SEP)
        } else {
            sb.append('>').append(LINE_SEP)
            for (subElement in outline.subElements) {
                writeOutline(sb, subElement)
            }
            sb.append(DOUBLE_INDENT).append("</outline>").append(LINE_SEP)
        }
    }

    private fun escapeText(s: String): String {
        if (s.isEmpty()) return s
        val sb = StringBuilder(s.length)
        for (c in s) {
            when (c) {
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '&' -> sb.append("&amp;")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun escapeAttr(s: String): String {
        if (s.isEmpty()) return s
        val sb = StringBuilder(s.length)
        for (c in s) {
            when (c) {
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '&' -> sb.append("&amp;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    companion object {
        private const val LINE_SEP = "\n"
        private const val SINGLE_INDENT = "  "
        private const val DOUBLE_INDENT = "    "
    }
}
