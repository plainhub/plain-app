package com.ismartcoding.lib.apk.parser

import com.ismartcoding.lib.apk.struct.xml.XmlCData
import com.ismartcoding.lib.apk.struct.xml.XmlNamespaceEndTag
import com.ismartcoding.lib.apk.struct.xml.XmlNamespaceStartTag
import com.ismartcoding.lib.apk.struct.xml.XmlNodeEndTag
import com.ismartcoding.lib.apk.struct.xml.XmlNodeStartTag

interface XmlStreamer {
    fun onStartTag(xmlNodeStartTag: XmlNodeStartTag)
    fun onEndTag(xmlNodeEndTag: XmlNodeEndTag)
    fun onCData(xmlCData: XmlCData)
    fun onNamespaceStart(tag: XmlNamespaceStartTag)
    fun onNamespaceEnd(tag: XmlNamespaceEndTag)
}