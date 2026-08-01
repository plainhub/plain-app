package com.ismartcoding.plain.lib.readability4j.processor

import com.ismartcoding.plain.lib.html2md.HtmlElement
import com.ismartcoding.plain.lib.html2md.HtmlNode
import com.ismartcoding.plain.lib.html2md.HtmlTextNode
import com.ismartcoding.plain.lib.readability4j.RegExUtil

object ProcessorHelper {
    fun removeNodes(element: HtmlElement, tagName: String, filterFunction: ((HtmlElement) -> Boolean)? = null) {
        element.getElementsByTag(tagName).reversed().forEach { childElement ->
            if (childElement.parentNode() != null) {
                if (filterFunction == null || filterFunction(childElement)) {
                    printAndRemove(childElement, "removeNode('$tagName')")
                }
            }
        }
    }

    fun printAndRemove(node: HtmlNode, reason: String) {
        if (node.parent() != null) {
            node.remove()
        }
    }

    fun replaceNodes(parentElement: HtmlElement, tagName: String, newTagName: String) {
        parentElement.getElementsByTag(tagName).forEach { element ->
            element.tagName(newTagName)
        }
    }

    fun nextElement(node: HtmlNode?): HtmlElement? {
        var next: HtmlNode? = node

        while (next != null && next !is HtmlElement && (next is HtmlTextNode && RegExUtil.isWhitespace(next.text()))) {
            next = next.nextSibling()
        }

        return next as? HtmlElement
    }

    fun getInnerText(e: HtmlElement, normalizeSpaces: Boolean = true): String {
        val textContent = e.text().trim()

        if (normalizeSpaces) {
            return RegExUtil.normalize(textContent)
        }

        return textContent
    }

}
