package com.ismartcoding.plain.lib.html2md

import com.ismartcoding.plain.lib.html2md.NodeUtils.isNodeType1
import com.ismartcoding.plain.lib.html2md.NodeUtils.isNodeType3

class MDConverter {
    private val rules = Rules()

    fun convert(input: String): String {
        rules.references.clear()
        return postProcess(process(ProcessNode(input)))
    }

    private class Escape(var pattern: String, var replace: String)

    private val escapes =
        listOf(
            Escape("\\\\", "\\\\\\\\"),
            Escape("\\*", "\\\\*"),
            Escape("^-", "\\\\-"),
            Escape("^\\+ ", "\\\\+ "),
            Escape("^(=+)", "\\\\$1"),
            Escape("^(#{1,6}) ", "\\\\$1 "),
            Escape("`", "\\\\`"),
            Escape("^~~~", "\\\\~~~"),
            Escape("\\[", "\\\\["),
            Escape("\\]", "\\\\]"),
            Escape("^>", "\\\\>"),
            Escape("_", "\\\\_"),
            Escape("^(\\d+)\\. ", "$1\\\\. "),
        )

    private fun postProcess(output: String): String {
        var o = output
        for (rule in rules.rules) {
            if (rule.append != null) {
                o = join(o, rule.append!!())
            }
        }
        return o.replace("^[\\t\\n\\r]+".toRegex(), "").replace("[\\t\\r\\n\\s]+$".toRegex(), "")
    }

    private fun process(node: ProcessNode): String {
        var result = ""
        for (child in node.element.childNodes()) {
            val processNodeChild = ProcessNode(child, node)
            var replacement = ""
            if (isNodeType3(child)) {
                replacement = if (processNodeChild.isCode) (child as HtmlTextNode).text() else escape((child as HtmlTextNode).text())
            } else if (isNodeType1(child)) {
                replacement = replacementForNode(processNodeChild)
            }
            result = join(result, replacement)
        }
        return result
    }

    private fun replacementForNode(node: ProcessNode): String {
        val rule = rules.findRule(node.element)
        var content = process(node)
        val flankingWhiteSpaces = node.flankingWhitespace()
        if (flankingWhiteSpaces.leading.isNotEmpty() || flankingWhiteSpaces.trailing.isNotEmpty()) {
            content = content.trim { it <= ' ' }
        }
        return (
            flankingWhiteSpaces.leading + rule!!.replacement(content, node.element) +
                flankingWhiteSpaces.trailing
        )
    }

    private fun join(
        string1: String,
        string2: String,
    ): String {
        val trailingNewlines = Regex("(\\n*)$").find(string1)?.value ?: ""
        val leadingNewlines = Regex("^(\\n*)").find(string2)?.value ?: ""
        val nNewLines = minOf(2, maxOf(leadingNewlines.length, trailingNewlines.length))
        val newLineJoin = "\n".repeat(nNewLines)
        return string1.dropLast(trailingNewlines.length) + newLineJoin + string2.drop(leadingNewlines.length)
    }

    private fun escape(string: String): String {
        var s = string
        for (escape in escapes) {
            s = s.replace(escape.pattern.toRegex(), escape.replace)
        }
        return s
    }
}
