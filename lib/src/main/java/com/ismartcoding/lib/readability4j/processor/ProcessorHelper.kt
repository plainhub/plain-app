package com.ismartcoding.lib.readability4j.processor

import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.readability4j.RegExUtil
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

object ProcessorHelper {
    fun removeNodes(element: Element, tagName: String, filterFunction: ((Element) -> Boolean)? = null) {
        element.getElementsByTag(tagName).reversed().forEach { childElement ->
            if (childElement.parentNode() != null) {
                if (filterFunction == null || filterFunction(childElement)) {
                    printAndRemove(childElement, "removeNode('$tagName')")
                }
            }
        }
    }

    fun printAndRemove(node: Node, reason: String) {
        if (node.parent() != null) {
            logNodeInfo(node, reason)
            node.remove()
        }
    }

    private fun logNodeInfo(node: Node, reason: String) {
        val nodeToString = "\n------\n" + node.outerHtml() + "\n------\n"
        LogCat.d("$reason, $nodeToString")
    }

    fun replaceNodes(parentElement: Element, tagName: String, newTagName: String) {
        parentElement.getElementsByTag(tagName).forEach { element ->
            element.tagName(newTagName)
        }
    }


    /**
     * Finds the next element, starting from the given node, and ignoring
     * whitespace in between. If the given node is an element, the same node is
     * returned.
     */
    fun nextElement(node: Node?): Element? {
        var next: Node? = node

        while (next != null && next !is Element && (next is TextNode && RegExUtil.isWhitespace(next.text()))) {
            next = next.nextSibling()
        }

        return next as? Element
    }

    /**
     * Get the inner text of a node - cross browser compatibly.
     * This also strips out any excess whitespace to be found.
     */
    fun getInnerText(e: Element, normalizeSpaces: Boolean = true): String {
        val textContent = e.text().trim()

        if (normalizeSpaces) {
            return RegExUtil.normalize(textContent)
        }

        return textContent
    }

}
