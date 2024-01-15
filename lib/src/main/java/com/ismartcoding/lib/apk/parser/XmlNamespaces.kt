package com.ismartcoding.lib.apk.parser

import com.ismartcoding.lib.apk.struct.xml.XmlNamespaceEndTag
import com.ismartcoding.lib.apk.struct.xml.XmlNamespaceStartTag

internal class XmlNamespaces {
    private val namespaces: MutableList<XmlNamespace>
    private val newNamespaces: MutableList<XmlNamespace>

    init {
        namespaces = ArrayList()
        newNamespaces = ArrayList()
    }

    fun addNamespace(tag: XmlNamespaceStartTag) {
        val namespace = XmlNamespace(tag.prefix, tag.uri)
        namespaces.add(namespace)
        newNamespaces.add(namespace)
    }

    fun removeNamespace(tag: XmlNamespaceEndTag) {
        val namespace = XmlNamespace(tag.prefix, tag.uri)
        namespaces.remove(namespace)
        newNamespaces.remove(namespace)
    }

    fun getPrefixViaUri(uri: String?): String? {
        if (uri == null) {
            return null
        }
        for (namespace in namespaces) {
            if (namespace.uri == uri) {
                return namespace.prefix
            }
        }
        return null
    }

    fun consumeNameSpaces(): List<XmlNamespace> {
        return if (!newNamespaces.isEmpty()) {
            val xmlNamespaces: MutableList<XmlNamespace> =
                ArrayList()
            xmlNamespaces.addAll(newNamespaces)
            newNamespaces.clear()
            xmlNamespaces
        } else {
            emptyList()
        }
    }

    /**
     * one namespace
     */
    class XmlNamespace(val prefix: String?, val uri: String?) {

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val namespace = o as XmlNamespace
            if (prefix == null && namespace.prefix != null) return false
            if (uri == null && namespace.uri != null) return false
            if (prefix != null && prefix != namespace.prefix) return false
            return if (uri != null && uri != namespace.uri) false else true
        }

        override fun hashCode(): Int {
            var result = prefix.hashCode()
            result = 31 * result + uri.hashCode()
            return result
        }
    }
}