package com.ismartcoding.lib.html2md

import com.ismartcoding.lib.html2md.NodeUtils.isNodeType1
import com.ismartcoding.lib.html2md.NodeUtils.isNodeType3
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.util.regex.Pattern

internal class WhitespaceCollapser {
    /**
     * Remove extraneous whitespace from the given element. Modifies the node in place
     * @param element
     */
    fun collapse(element: Node) {
        if (element.childNodeSize() == 0 || isPre(element)) {
            return
        }
        var prevText: TextNode? = null
        var prevVoid = false
        var prev: Node? = null
        var node = next(prev, element)

        // Traverse the tree
        while (node != element) {
            if (isNodeType3(node!!)) {
                val textNode = node as TextNode?
                var value = textNode!!.attributes()["#text"].replace("[ \\r\\n\\t]+".toRegex(), " ")
                if ((prevText == null || Pattern.compile(" $").matcher(prevText.text()).find()) && !prevVoid && value[0] == ' ') {
                    value = value.substring(1)
                }
                if (value.isEmpty()) {
                    node = remove(node)
                    continue
                }
                val newNode = TextNode(value)
                node.replaceWith(newNode)
                prevText = newNode
                node = newNode
            } else if (isNodeType1(node)) {
                if (isBlock(node)) {
                    prevText?.text(prevText.text().replace(" $".toRegex(), ""))
                    prevText = null
                    prevVoid = false
                } else if (isVoid(node)) {
                    // avoid trimming space around non block, non br void elements
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

    /**
     * remove(node) removes the given node from the DOM and returns the
     * next node in the sequence.
     *
     * @param {Node} node
     * @return {Node} node
     */
    private fun remove(node: Node): Node? {
        val next = if (node.nextSibling() != null) node.nextSibling() else node.parentNode()
        node.remove()
        return next
    }

    /**
     * Returns next node in the sequence given current and previous nodes
     */
    private fun next(
        prev: Node?,
        current: Node,
    ): Node? {
        if (prev != null && prev.parent() == current || isPre(current)) {
            // TODO beware parentNode might not be element
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

    private fun isPre(element: Node): Boolean {
        return element.nodeName() == "pre"
    }

    private fun isBlock(element: Node): Boolean {
        return ProcessNode.isBlock(element) || element.nodeName() == "br"
    }

    private fun isVoid(element: Node): Boolean {
        // Allow to override
        return ProcessNode.isVoid(element)
    }
}
