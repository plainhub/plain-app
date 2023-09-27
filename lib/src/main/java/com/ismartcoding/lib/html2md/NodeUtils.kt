package com.ismartcoding.lib.html2md

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

internal object NodeUtils {
    fun isNodeType1(element: Node): Boolean {
        return element is Element
    }

    fun isNodeType3(element: Node): Boolean {
        return element.nodeName() == "text" || element.nodeName() == "#text"
    }
}
