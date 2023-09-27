package com.ismartcoding.lib.html2md

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

internal object Table {
    fun tableCell(): Rule {
        return Rule(setOf("th", "td")) { content: String, node: Node ->
            cell(content, node) + spannedCells(node, "")
        }
    }

    fun tableRow(): Rule {
        return Rule(setOf("tr")) { content: String, node: Node ->
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
                    val align = n.attr("align").lowercase()
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
        return Rule({ node: Node -> node.nodeName().equals("table", true) && !isNestedTable(node) }, { content: String, _: Node ->
            "\n\n" + content.replace("\n\n", "\n") + "\n\n"
        })
    }

    fun tableSection(): Rule {
        return Rule(setOf("thead", "tbody", "tfoot")) { content: String, _: Node ->
            content
        }
    }

    fun tableCaption(): Rule {
        return Rule(setOf("caption")) { content: String, node: Node ->
            val parentNode = node.parentNode()
            if (parentNode?.nodeName() == "table" && parentNode.childNode(0) == node) {
                content
            } else {
                ""
            }
        }
    }

    private fun isHeadingRow(tr: Node): Boolean {
        val parentNode = tr.parentNode()
        var tableNode = parentNode
        if (parentNode != null &&
            setOf("thead", "tfoot", "tbody").contains(parentNode.nodeName().lowercase())
        ) {
            tableNode = parentNode.parentNode()
        }
        return (tableNode?.nodeName().equals("table", true) && (tableNode as Element).select("tr").firstOrNull() == tr)
    }

    private fun cell(
        content: String,
        node: Node,
    ): String {
        val parent = node.parentNode() as Element
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
        node: Node,
        content: String,
    ): String {
        val colSpan = node.attr("colspan").toIntOrNull() ?: 1
        if (colSpan <= 1) {
            return ""
        }

        return (" $content | ").repeat(colSpan - 1)
    }

    private fun isNestedTable(node: Node): Boolean {
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
