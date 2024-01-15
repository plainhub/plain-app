package com.ismartcoding.lib.apk.parser

import com.ismartcoding.lib.apk.struct.xml.XmlCData
import com.ismartcoding.lib.apk.struct.xml.XmlNamespaceEndTag
import com.ismartcoding.lib.apk.struct.xml.XmlNamespaceStartTag
import com.ismartcoding.lib.apk.struct.xml.XmlNodeEndTag
import com.ismartcoding.lib.apk.struct.xml.XmlNodeStartTag

class CompositeXmlStreamer(vararg xmlStreamers: XmlStreamer) : XmlStreamer {
    @Suppress("UNCHECKED_CAST")
    private val xmlStreamers: Array<XmlStreamer> = xmlStreamers as Array<XmlStreamer>

    override fun onStartTag(xmlNodeStartTag: XmlNodeStartTag) {
        for (xmlStreamer in xmlStreamers) {
            xmlStreamer.onStartTag(xmlNodeStartTag)
        }
    }

    override fun onEndTag(xmlNodeEndTag: XmlNodeEndTag) {
        for (xmlStreamer in xmlStreamers) {
            xmlStreamer.onEndTag(xmlNodeEndTag)
        }
    }

    override fun onCData(xmlCData: XmlCData) {
        for (xmlStreamer in xmlStreamers) {
            xmlStreamer.onCData(xmlCData)
        }
    }

    override fun onNamespaceStart(tag: XmlNamespaceStartTag) {
        for (xmlStreamer in xmlStreamers) {
            xmlStreamer.onNamespaceStart(tag)
        }
    }

    override fun onNamespaceEnd(tag: XmlNamespaceEndTag) {
        for (xmlStreamer in xmlStreamers) {
            xmlStreamer.onNamespaceEnd(tag)
        }
    }
}