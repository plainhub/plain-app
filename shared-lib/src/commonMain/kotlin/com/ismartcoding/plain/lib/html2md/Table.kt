package com.ismartcoding.plain.lib.html2md

internal object Table {
    fun tableCell(): Rule {
        return Rule(setOf("th", "td")) { content: String, node: HtmlNode ->
            cell(content, node) + spannedCells(node, "")
        }
    }

    fun tableRow(): Rule {
        return Rule(setOf("tr")) { content: String, node: HtmlNode ->
            val alignMap =
                mapOf(
                    "left" to ":--",
                    "right" to "--:",
                    "center" to ":-:",
                )

            var borderCells = ""
            if (isHeadingRow(node)) {
                node.childNodes().forEach { n ->
                    var border = "---"
                    val align = if (n is HtmlElement) n.attr("align").lowercase() else ""
                    if (align.isNotEmpty()) {
                        border = alignMap[align] ?: border
                    }
                    borderCells += cell(border, n) + spannedCells(n, border)
                }
            }
            "\n" + content + (
                if (borderCells.isNotEmpty()) {
                    "\n" + borderCells
                } else {
                    ""
                }
            )
        }
    }

    fun table(): Rule {
        return Rule({ node: HtmlNode -> node.nodeName() == "table" && !isNestedTable(node) }, { content: String, _: HtmlNode ->
            "\n\n" + content.replace("\n\n", "\n") + "\n\n"
        })
    }

    fun tableSection(): Rule {
        return Rule(setOf("thead", "tbody", "tfoot")) { content: String, _: HtmlNode ->
            content
        }
    }

    fun tableCaption(): Rule {
        return Rule(setOf("caption")) { content: String, node: HtmlNode ->
            val parentNode = node.parentNode()
            if (parentNode?.nodeName() == "table" && parentNode.childNode(0) === node) {
                content
            } else {
                ""
            }
        }
    }

    private fun isHeadingRow(tr: HtmlNode): Boolean {
        val parentNode = tr.parentNode()
        var tableNode = parentNode
        if (parentNode != null &&
            setOf("thead", "tfoot", "tbody").contains(parentNode.nodeName().lowercase())
        ) {
            tableNode = parentNode.parentNode()
        }
        return tableNode?.nodeName()?.equals("table", true) == true &&
            (tableNode as HtmlElement).select("tr").firstOrNull() === tr
    }

    private fun cell(
        content: String,
        node: HtmlNode,
    ): String {
        val parent = node.parentNode() as HtmlElement
        val index = parent.childNodes().indexOf(node)
        var prefix = " "
        if (index == 0) {
            prefix = "| "
        }

        var c = content.replace("\r\n", "\n").replace("\n", " ")
        c = c.replace("|", "\\|")
        return "$prefix$c |"
    }

    private fun spannedCells(
        node: HtmlNode,
        content: String,
    ): String {
        val colSpan = if (node is HtmlElement) node.attr("colspan").toIntOrNull() ?: 1 else 1
        if (colSpan <= 1) {
            return ""
        }

        return (" $content | ").repeat(colSpan - 1)
    }

    private fun isNestedTable(node: HtmlNode): Boolean {
        var current = node.parentNode()
        while (current != null) {
            if (current.nodeName().equals("table", true)) {
                return true
            }
            current = current.parentNode()
        }

        return false
    }
}
