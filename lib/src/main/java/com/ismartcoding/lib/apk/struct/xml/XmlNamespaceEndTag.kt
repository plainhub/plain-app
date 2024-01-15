package com.ismartcoding.lib.apk.struct.xml

class XmlNamespaceEndTag(
    @JvmField val prefix: String,
    @JvmField val uri: String
) {
    override fun toString(): String {
        return "$prefix=$uri"
    }
}