package com.ismartcoding.lib.gsonxml

import com.google.gson.GsonBuilder

class GsonXmlBuilder {
    private val coreBuilder = GsonBuilder()

    private val options =
        XmlReader.Options().apply {
            // Parse option: whether to skip root element
            skipRoot = true
            // Parse option: whether to treat XML namespaces.
            namespaces = false
            // Parse option: list a created from a set of elements with the same name without a grouping element.
            sameNameList = false
        }

    fun create(): GsonXml {
        return GsonXml(coreBuilder.create(), options)
    }
}
