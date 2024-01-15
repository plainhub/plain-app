package com.ismartcoding.lib.apk.struct.xml

class XmlNamespaceStartTag {
    var prefix: String? = null
    var uri: String? = null
    override fun toString(): String {
        return "$prefix=$uri"
    }
}