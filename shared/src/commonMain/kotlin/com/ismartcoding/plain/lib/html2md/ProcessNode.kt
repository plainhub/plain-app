package com.ismartcoding.plain.lib.html2md

import com.ismartcoding.plain.lib.html2md.NodeUtils.isNodeType1
import com.ismartcoding.plain.lib.html2md.NodeUtils.isNodeType3

internal class ProcessNode {
    var element: HtmlNode
    var parent: ProcessNode? = null

    constructor(input: String) {
        val document = HtmlParser.parse(
            "<x-html2md id=\"html2md-root\">$input</x-html2md>",
        )
        val root = document.getElementById("html2md-root")!!
        WhitespaceCollapser().collapse(root)
        element = root
    }

    constructor(node: HtmlNode, parent: ProcessNode?) {
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
                if (element is HtmlElement) {
                    (element as HtmlElement).wholeText()
                } else {
                    element.outerHtml()
                }
            if (textContent == "\n") {
                return FlankingWhiteSpaces("", "")
            }
            val hasLeading = Regex("^\\s").containsMatchIn(textContent)
            val hasTrailing = Regex("\\s$").containsMatchIn(textContent)
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
        sibling: HtmlNode?,
    ): Boolean {
        if (sibling == null) {
            return false
        }
        if (isNodeType3(sibling) || isNodeType1(sibling)) {
            return Regex(regex).containsMatchIn(sibling.outerHtml())
        }
        return false
    }

    private fun hasBlockNodesSet(node: HtmlNode): Boolean {
        if (node !is HtmlElement) {
            return false
        }
        for (tagName in BLOCK_ELEMENTS) {
            if (node.getElementsByTag(tagName).isNotEmpty()) {
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

        fun isBlank(element: HtmlNode): Boolean {
            val textContent: String =
                if (element is HtmlElement) {
                    element.wholeText()
                } else {
                    element.outerHtml()
                }
            return !isVoid(element) &&
                !isMeaningfulWhenBlank(element) &&
                textContent.isBlank() &&
                !hasVoidNodesSet(element) &&
                !hasMeaningfulWhenBlankNodesSet(element)
        }

        private fun hasVoidNodesSet(node: HtmlNode?): Boolean {
            if (node !is HtmlElement) {
                return false
            }
            for (tagName in VOID_ELEMENTS) {
                if (node.getElementsByTag(tagName).isNotEmpty()) {
                    return true
                }
            }
            return false
        }

        fun isVoid(element: HtmlNode): Boolean {
            return VOID_ELEMENTS.contains(element.nodeName())
        }

        private fun hasMeaningfulWhenBlankNodesSet(node: HtmlNode): Boolean {
            if (node !is HtmlElement) {
                return false
            }
            for (tagName in MEANINGFUL_WHEN_BLANK_ELEMENTS) {
                if (node.getElementsByTag(tagName).isNotEmpty()) {
                    return true
                }
            }
            return false
        }

        private fun isMeaningfulWhenBlank(element: HtmlNode): Boolean {
            return MEANINGFUL_WHEN_BLANK_ELEMENTS.contains(element.nodeName())
        }

        fun isBlock(element: HtmlNode): Boolean {
            return BLOCK_ELEMENTS.contains(element.nodeName())
        }
    }
}
