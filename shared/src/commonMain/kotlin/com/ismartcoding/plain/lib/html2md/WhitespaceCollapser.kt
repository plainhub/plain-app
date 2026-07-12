package com.ismartcoding.plain.lib.html2md

import com.ismartcoding.plain.lib.html2md.NodeUtils.isNodeType1
import com.ismartcoding.plain.lib.html2md.NodeUtils.isNodeType3

internal class WhitespaceCollapser {
    fun collapse(element: HtmlNode) {
        if (element.childNodeSize() == 0 || isPre(element)) {
            return
        }
        var prevText: HtmlTextNode? = null
        var prevVoid = false
        var prev: HtmlNode? = null
        var node = next(prev, element)

        while (node !== element) {
            if (isNodeType3(node!!)) {
                val textNode = node as HtmlTextNode
                var value = textNode.text().replace("[ \\r\\n\\t]+".toRegex(), " ")
                if ((prevText == null || Regex(" $").containsMatchIn(prevText.text())) && !prevVoid && value.isNotEmpty() && value[0] == ' ') {
                    value = value.substring(1)
                }
                if (value.isEmpty()) {
                    node = remove(node)
                    continue
                }
                val newNode = HtmlTextNode(value)
                node.replaceWith(newNode)
                prevText = newNode
                node = newNode
            } else if (isNodeType1(node)) {
                if (isBlock(node)) {
                    prevText?.text(prevText.text().replace(" $".toRegex(), ""))
                    prevText = null
                    prevVoid = false
                } else if (isVoid(node)) {
                    prevText = null
                    prevVoid = true
                }
            } else {
                node = remove(node)
                continue
            }
            val nextNode = next(prev, node)
            prev = node
            node = nextNode
        }
        if (prevText != null) {
            prevText.text(prevText.text().replace(" $".toRegex(), ""))
            if (prevText.text().isEmpty()) {
                remove(prevText)
            }
        }
    }

    private fun remove(node: HtmlNode): HtmlNode? {
        val next = if (node.nextSibling() != null) node.nextSibling() else node.parentNode()
        node.remove()
        return next
    }

    private fun next(
        prev: HtmlNode?,
        current: HtmlNode,
    ): HtmlNode? {
        if (prev != null && prev.parent() === current || isPre(current)) {
            return if (current.nextSibling() != null) current.nextSibling() else current.parentNode()
        }
        if (current.childNodeSize() != 0) {
            return current.childNode(0)
        }
        return if (current.nextSibling() != null) {
            current.nextSibling()
        } else {
            current.parentNode()
        }
    }

    private fun isPre(element: HtmlNode): Boolean {
        return element.nodeName() == "pre"
    }

    private fun isBlock(element: HtmlNode): Boolean {
        return ProcessNode.isBlock(element) || element.nodeName() == "br"
    }

    private fun isVoid(element: HtmlNode): Boolean {
        return ProcessNode.isVoid(element)
    }
}
