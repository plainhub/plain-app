package com.ismartcoding.lib.opml

import com.ismartcoding.lib.opml.entity.Body
import com.ismartcoding.lib.opml.entity.Head
import com.ismartcoding.lib.opml.entity.Opml
import com.ismartcoding.lib.opml.entity.Outline
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.StringWriter
import java.nio.charset.StandardCharsets

class OpmlWriter {
    fun write(opml: Opml): String {
        val factory = XmlPullParserFactory.newInstance()
        val serializer = factory.newSerializer()
        val stringWriter = StringWriter()
        serializer.setOutput(stringWriter)
        serializer.startDocument(StandardCharsets.UTF_8.name(), null)
        serializer.text(System.lineSeparator())
        writeOpml(serializer, opml)
        serializer.endDocument()
        return stringWriter.toString()
    }

    private fun writeOpml(
        serializer: XmlSerializer,
        opml: Opml,
    ) {
        serializer.startTag(null, "opml")
        serializer.attribute(null, "version", opml.version)
        serializer.text(System.lineSeparator())
        writeHead(serializer, opml.head)
        writeBody(serializer, opml.body)
        serializer.endTag(null, "opml")
        serializer.text(System.lineSeparator())
    }

    private fun writeHead(
        serializer: XmlSerializer,
        head: Head,
    ) {
        serializer.text(SINGLE_INDENT)
        serializer.startTag(null, "head")
        serializer.text(System.lineSeparator())
        if (head.title.isNotBlank()) {
            serializer.text(DOUBLE_INDENT)
            serializer.startTag(null, "title")
            serializer.text(head.title)
            serializer.endTag(null, "title")
            serializer.text(System.lineSeparator())
        }
        if (head.dateCreated.isNotBlank()) {
            serializer.text(DOUBLE_INDENT)
            serializer.startTag(null, "dateCreated")
            serializer.text(head.dateCreated)
            serializer.endTag(null, "dateCreated")
            serializer.text(System.lineSeparator())
        }
        if (head.dateModified.isNotBlank()) {
            serializer.text(DOUBLE_INDENT)
            serializer.startTag(null, "dateModified")
            serializer.text(head.dateModified)
            serializer.endTag(null, "dateModified")
            serializer.text(System.lineSeparator())
        }
        if (head.ownerName.isNotBlank()) {
            serializer.text(DOUBLE_INDENT)
            serializer.startTag(null, "ownerName")
            serializer.text(head.ownerName)
            serializer.endTag(null, "ownerName")
            serializer.text(System.lineSeparator())
        }
        if (head.ownerEmail.isNotBlank()) {
            serializer.text(DOUBLE_INDENT)
            serializer.startTag(null, "ownerEmail")
            serializer.text(head.ownerEmail)
            serializer.endTag(null, "ownerEmail")
            serializer.text(System.lineSeparator())
        }
        if (head.ownerId.isNotBlank()) {
            serializer.text(DOUBLE_INDENT)
            serializer.startTag(null, "ownerId")
            serializer.text(head.ownerId)
            serializer.endTag(null, "ownerId")
            serializer.text(System.lineSeparator())
        }
        if (head.docs.isNotBlank()) {
            serializer.text(DOUBLE_INDENT)
            serializer.startTag(null, "docs")
            serializer.text(head.docs)
            serializer.endTag(null, "docs")
            serializer.text(System.lineSeparator())
        }
        if (head.expansionState.isNotEmpty()) {
            serializer.text(DOUBLE_INDENT)
            serializer.startTag(null, "expansionState")
            serializer.text(head.expansionStateString)
            serializer.endTag(null, "expansionState")
            serializer.text(System.lineSeparator())
        }
        if (head.vertScrollState != null) {
            serializer.text(DOUBLE_INDENT)
            serializer.startTag(null, "vertScrollState")
            serializer.text(head.vertScrollState.toString())
            serializer.endTag(null, "vertScrollState")
            serializer.text(System.lineSeparator())
        }
        if (head.windowTop != null) {
            serializer.text(DOUBLE_INDENT)
            serializer.startTag(null, "windowTop")
            serializer.text(head.windowTop.toString())
            serializer.endTag(null, "windowTop")
            serializer.text(System.lineSeparator())
        }
        if (head.windowLeft != null) {
            serializer.text(DOUBLE_INDENT)
            serializer.startTag(null, "windowLeft")
            serializer.text(head.windowLeft.toString())
            serializer.endTag(null, "windowLeft")
            serializer.text(System.lineSeparator())
        }
        if (head.windowBottom != null) {
            serializer.text(DOUBLE_INDENT)
            serializer.startTag(null, "windowBottom")
            serializer.text(head.windowBottom.toString())
            serializer.endTag(null, "windowBottom")
            serializer.text(System.lineSeparator())
        }
        if (head.windowRight != null) {
            serializer.text(DOUBLE_INDENT)
            serializer.startTag(null, "windowRight")
            serializer.text(head.windowRight.toString())
            serializer.endTag(null, "windowRight")
            serializer.text(System.lineSeparator())
        }
        serializer.text(SINGLE_INDENT)
        serializer.endTag(null, "head")
        serializer.text(System.lineSeparator())
    }

    private fun writeBody(
        serializer: XmlSerializer,
        body: Body,
    ) {
        serializer.text(SINGLE_INDENT)
        serializer.startTag(null, "body")
        serializer.text(System.lineSeparator())
        for (outline in body.outlines) {
            writeOutline(serializer, outline)
        }
        serializer.text(SINGLE_INDENT)
        serializer.endTag(null, "body")
        serializer.text(System.lineSeparator())
    }

    private fun writeOutline(
        serializer: XmlSerializer,
        outline: Outline,
    ) {
        serializer.text(DOUBLE_INDENT)
        serializer.startTag(null, "outline")
        for ((key, value) in outline.attributes.entries) {
            serializer.attribute(null, key, value)
        }
        for (subElement in outline.subElements) {
            writeOutline(serializer, subElement)
        }
        serializer.endTag(null, "outline")
        serializer.text(System.lineSeparator())
    }

    companion object {
        private const val SINGLE_INDENT = "  "
        private const val DOUBLE_INDENT = "    "
    }
}
