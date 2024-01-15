package com.ismartcoding.lib.apk.parser

import com.ismartcoding.lib.apk.struct.xml.Attribute
import com.ismartcoding.lib.apk.struct.xml.XmlCData
import com.ismartcoding.lib.apk.struct.xml.XmlNamespaceEndTag
import com.ismartcoding.lib.apk.struct.xml.XmlNamespaceStartTag
import com.ismartcoding.lib.apk.struct.xml.XmlNodeEndTag
import com.ismartcoding.lib.apk.struct.xml.XmlNodeStartTag
import com.ismartcoding.lib.apk.utils.xml.XmlEscaper

class XmlTranslator : XmlStreamer {
    private val sb: StringBuilder = StringBuilder()
    private var shift = 0
    private val namespaces: XmlNamespaces
    private var isLastStartTag = false

    init {
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        namespaces = XmlNamespaces()
    }

    override fun onStartTag(xmlNodeStartTag: XmlNodeStartTag) {
        if (isLastStartTag) {
            sb.append(">\n")
        }
        appendShift(shift++)
        sb.append('<')
        if (xmlNodeStartTag.namespace != null) {
            val prefix = namespaces.getPrefixViaUri(xmlNodeStartTag.namespace)
            if (prefix != null) {
                sb.append(prefix).append(":")
            } else {
                sb.append(xmlNodeStartTag.namespace).append(":")
            }
        }
        sb.append(xmlNodeStartTag.name)
        val nps = namespaces.consumeNameSpaces()
        if (!nps.isEmpty()) {
            for (np in nps) {
                sb.append(" xmlns:").append(np.prefix).append("=\"")
                    .append(np.uri)
                    .append("\"")
            }
        }
        isLastStartTag = true
        for (attribute in xmlNodeStartTag.attributes?.attributes!!) {
            attribute?.let { onAttribute(it) }
        }
    }

    private fun onAttribute(attribute: Attribute) {
        sb.append(" ")
        var namespace = namespaces.getPrefixViaUri(attribute.namespace)
        if (namespace == null) {
            namespace = attribute.namespace
        }
        if (namespace.isNotEmpty()) {
            sb.append(namespace).append(':')
        }
        val escapedFinalValue = XmlEscaper.escapeXml10(attribute.value)
        sb.append(attribute.name).append('=').append('"').append(escapedFinalValue).append('"')
    }

    override fun onEndTag(xmlNodeEndTag: XmlNodeEndTag) {
        --shift
        if (isLastStartTag) {
            sb.append(" />\n")
        } else {
            appendShift(shift)
            sb.append("</")
            if (xmlNodeEndTag.namespace != null) {
                var namespace = namespaces.getPrefixViaUri(xmlNodeEndTag.namespace)
                if (namespace == null) {
                    namespace = xmlNodeEndTag.namespace
                }
                sb.append(namespace).append(":")
            }
            sb.append(xmlNodeEndTag.name)
            sb.append(">\n")
        }
        isLastStartTag = false
    }

    override fun onCData(xmlCData: XmlCData) {
        appendShift(shift)
        sb.append(xmlCData.value).append('\n')
        isLastStartTag = false
    }

    override fun onNamespaceStart(tag: XmlNamespaceStartTag) {
        namespaces.addNamespace(tag)
    }

    override fun onNamespaceEnd(tag: XmlNamespaceEndTag) {
        namespaces.removeNamespace(tag)
    }

    private fun appendShift(shift: Int) {
        for (i in 0 until shift) {
            sb.append("\t")
        }
    }

    val xml: String
        get() = sb.toString()
}