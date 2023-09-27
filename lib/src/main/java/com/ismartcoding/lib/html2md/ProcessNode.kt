package com.ismartcoding.lib.html2md

import com.ismartcoding.lib.html2md.NodeUtils.isNodeType1
import com.ismartcoding.lib.html2md.NodeUtils.isNodeType3
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import java.util.regex.Pattern

internal class ProcessNode {
    var element: Node
    var parent: ProcessNode? = null

    constructor(input: String) {
        val document =
            Jsoup.parse( // DOM parsers arrange elements in the <head> and <body>.
                // Wrapping in a custom element ensures elements are reliably arranged in
                // a single element.
                "<x-html2md id=\"html2md-root\">$input</x-html2md>",
            )
        val root = document.getElementById("html2md-root")
        WhitespaceCollapser().collapse(root!!)
        element = root
    }

    constructor(node: Node, parent: ProcessNode?) {
        element = node
        this.parent = parent
    }

    val isCode: Boolean
        get() = element.nodeName() == "code" || parent?.isCode == true

    fun flankingWhitespace(): FlankingWhiteSpaces {
        var leading = ""
        var trailing = ""
        if (!isBlock(element)) {
            val textContent: String =
                if (element is Element) {
                    (element as Element).wholeText()
                } else {
                    element.outerHtml()
                }
            // Don't put extra spaces for a line break
            if (textContent == "\n") {
                return FlankingWhiteSpaces("", "")
            }
            // TODO original uses textContent
            val hasLeading = Pattern.compile("^\\s").matcher(textContent).find()
            val hasTrailing = Pattern.compile("\\s$").matcher(textContent).find()
            // TODO maybe make node property and avoid recomputing
            val blankWithSpaces = isBlank(element) && hasLeading && hasTrailing
            if (hasLeading && !isLeftFlankedByWhitespaces) {
                leading = " "
            }
            if (!blankWithSpaces && hasTrailing && !isRightFlankedByWhitespaces) {
                trailing = " "
            }
        }
        return FlankingWhiteSpaces(leading, trailing)
    }

    private val isLeftFlankedByWhitespaces: Boolean
        get() = isChildFlankedByWhitespaces(" $", element.previousSibling())
    private val isRightFlankedByWhitespaces: Boolean
        get() = isChildFlankedByWhitespaces("^ ", element.nextSibling())

    private fun isChildFlankedByWhitespaces(
        regex: String,
        sibling: Node?,
    ): Boolean {
        if (sibling == null) {
            return false
        }
        if (isNodeType3(sibling)) {
            // TODO fix. Originally sibling.nodeValue
            return Pattern.compile(regex).matcher(sibling.outerHtml()).find()
        }
        return if (isNodeType1(sibling)) {
            // TODO fix. Originally textContent
            Pattern.compile(regex).matcher(sibling.outerHtml()).find()
        } else {
            false
        }
    }

    private fun hasBlockNodesSet(node: Node): Boolean {
        if (node !is Element) {
            return false
        }
        for (tagName in BLOCK_ELEMENTS) {
            if (node.getElementsByTag(tagName).size != 0) {
                return true
            }
        }
        return false
    }

    internal class FlankingWhiteSpaces(val leading: String, val trailing: String)
    companion object {
        private val VOID_ELEMENTS =
            setOf(
                "area", "base", "br", "col", "command", "embed", "hr", "img", "input",
                "keygen", "link", "meta", "param", "source", "track", "wbr",
            )
        private val MEANINGFUL_WHEN_BLANK_ELEMENTS =
            setOf(
                "a", "table", "thead", "tbody", "tfoot", "th", "td", "iframe", "script",
                "audio", "video",
            )
        private val BLOCK_ELEMENTS =
            setOf(
                "address", "article", "aside", "audio", "blockquote", "body", "canvas",
                "center", "dd", "dir", "div", "dl", "dt", "fieldset", "figcaption", "figure",
                "footer", "form", "frameset", "h1", "h2", "h3", "h4", "h5", "h6", "header",
                "hgroup", "hr", "html", "isindex", "li", "main", "menu", "nav", "noframes",
                "noscript", "ol", "output", "p", "pre", "section", "table", "tbody", "td",
                "tfoot", "th", "thead", "tr", "ul",
            )

        fun isBlank(element: Node): Boolean {
            val textContent: String =
                if (element is Element) {
                    element.wholeText()
                } else {
                    element.outerHtml()
                }
            return !isVoid(element) &&
                !isMeaningfulWhenBlank(element) && // TODO check text is the same as textContent in browser
                Pattern.compile("(?i)^\\s*$").matcher(textContent).find() &&
                !hasVoidNodesSet(element) &&
                !hasMeaningfulWhenBlankNodesSet(element)
        }

        private fun hasVoidNodesSet(node: Node?): Boolean {
            if (node !is Element) {
                return false
            }
            for (tagName in VOID_ELEMENTS) {
                if (node.getElementsByTag(tagName).size != 0) {
                    return true
                }
            }
            return false
        }

        fun isVoid(element: Node): Boolean {
            return VOID_ELEMENTS.contains(element.nodeName())
        }

        private fun hasMeaningfulWhenBlankNodesSet(node: Node): Boolean {
            if (node !is Element) {
                return false
            }
            for (tagName in MEANINGFUL_WHEN_BLANK_ELEMENTS) {
                if (node.getElementsByTag(tagName).size != 0) {
                    return true
                }
            }
            return false
        }

        private fun isMeaningfulWhenBlank(element: Node): Boolean {
            return MEANINGFUL_WHEN_BLANK_ELEMENTS.contains(element.nodeName())
        }

        fun isBlock(element: Node): Boolean {
            return BLOCK_ELEMENTS.contains(element.nodeName())
        }
    }
}
