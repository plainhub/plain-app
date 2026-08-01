package com.ismartcoding.plain.lib.html2md

internal object NodeUtils {
    fun isNodeType1(element: HtmlNode): Boolean {
        return element is HtmlElement
    }

    fun isNodeType3(element: HtmlNode): Boolean {
        return element.nodeName() == "#text"
    }
}
