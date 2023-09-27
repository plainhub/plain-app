package com.ismartcoding.lib.html2md

import com.ismartcoding.lib.html2md.NodeUtils.isNodeType1
import com.ismartcoding.lib.html2md.NodeUtils.isNodeType3
import org.jsoup.nodes.TextNode
import java.util.*
import java.util.regex.Pattern

class MDConverter {
    private val rules = Rules()

    /**
     * Accepts a HTML string and converts it to markdown
     *
     * Note, if LinkStyle is chosen to be REFERENCED the method is not thread safe.
     * @param input html to be converted
     * @return markdown text
     */
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
                o = join(o, rule.append!!.get())
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
                // TODO it should be child.nodeValue
                replacement = if (processNodeChild.isCode) (child as TextNode).text() else escape((child as TextNode).text())
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
            flankingWhiteSpaces.leading + rule!!.replacement.apply(content, node.element) +
                flankingWhiteSpaces.trailing
        )
    }

    private fun join(
        string1: String,
        string2: String,
    ): String {
        val trailingMatcher = trailingNewLinePattern.matcher(string1)
        trailingMatcher.find()
        val leadingMatcher = leadingNewLinePattern.matcher(string2)
        leadingMatcher.find()
        val nNewLines = Integer.min(2, Integer.max(leadingMatcher.group().length, trailingMatcher.group().length))
        val newLineJoin = java.lang.String.join("", Collections.nCopies(nNewLines, "\n"))
        return (
            trailingMatcher.replaceAll("") +
                newLineJoin +
                leadingMatcher.replaceAll("")
        )
    }

    private fun escape(string: String): String {
        var s = string
        for (escape in escapes) {
            s = s.replace(escape.pattern.toRegex(), escape.replace)
        }
        return s
    }

    companion object {
        private val leadingNewLinePattern = Pattern.compile("^(\n*)")
        private val trailingNewLinePattern = Pattern.compile("(\n*)$")
    }
}
