package com.ismartcoding.plain.ui.base.mdeditor.blocks

/**
 * Splits markdown text into editing blocks. Regular text lines become TEXT blocks
 * (one per line) while structural units — fenced code, block math, tables and
 * standalone images — are kept whole as atomic blocks. An unclosed fence or math
 * delimiter absorbs the rest of the document, matching CommonMark behavior.
 * [parse] and [serialize] are inverse operations: parse(serialize(parse(x))) == parse(x).
 */

enum class MdBlockKind { TEXT, CODE, MATH, TABLE, IMAGE }

class ParsedBlock(val kind: MdBlockKind, val content: String)

object MdBlockParser {

    fun parse(text: String): List<ParsedBlock> {
        val lines = text.split('\n')
        val blocks = ArrayList<ParsedBlock>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val lead = line.trimStart(' ', '\t')
            val fence = fenceInfo(line)
            if (fence != null) {
                var j = i + 1
                while (j < lines.size && !isClosingFence(lines[j], fence)) j++
                val end = if (j < lines.size) j else lines.size - 1
                blocks.add(ParsedBlock(MdBlockKind.CODE, lines.subList(i, end + 1).joinToString("\n")))
                i = end + 1
                continue
            }
            if (lead == "$$") {
                var j = i + 1
                while (j < lines.size && lines[j].trimStart(' ', '\t') != "$$") j++
                val end = if (j < lines.size) j else lines.size - 1
                blocks.add(ParsedBlock(MdBlockKind.MATH, lines.subList(i, end + 1).joinToString("\n")))
                i = end + 1
                continue
            }
            if (SINGLE_MATH.matches(line)) {
                blocks.add(ParsedBlock(MdBlockKind.MATH, line))
                i++
                continue
            }
            if (lead.startsWith("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1])) {
                var k = i + 2
                while (k < lines.size && lines[k].trimStart(' ', '\t').startsWith("|")) k++
                blocks.add(ParsedBlock(MdBlockKind.TABLE, lines.subList(i, k).joinToString("\n")))
                i = k
                continue
            }
            if (IMAGE_LINE.matches(line)) {
                blocks.add(ParsedBlock(MdBlockKind.IMAGE, line))
                i++
                continue
            }
            blocks.add(ParsedBlock(MdBlockKind.TEXT, line))
            i++
        }
        return blocks
    }

    fun serialize(blocks: List<ParsedBlock>): String = blocks.joinToString("\n") { it.content }

    private fun fenceInfo(line: String): Pair<Char, Int>? {
        var i = 0
        while (i < line.length && i < 3 && line[i] == ' ') i++
        val rest = line.substring(i)
        val c = rest.firstOrNull() ?: return null
        if (c != '`' && c != '~') return null
        var n = 0
        while (n < rest.length && rest[n] == c) n++
        if (n < 3) return null
        return c to n
    }

    private fun isClosingFence(line: String, open: Pair<Char, Int>): Boolean {
        val trimmed = line.trim(' ', '\t')
        return trimmed.isNotEmpty() && trimmed.all { it == open.first } && trimmed.length >= open.second
    }

    private fun isTableSeparator(line: String): Boolean =
        line.contains('-') && TABLE_SEPARATOR.matches(line)

    private val SINGLE_MATH = Regex("^\\s*\\$\\$(.+)\\$\\$\\s*$")
    private val TABLE_SEPARATOR = Regex("^ *\\|? *:?-+:? *(?:\\| *:?-+:? *)*\\|? *$")
    private val IMAGE_LINE = Regex("^\\s*(?:!\\[[^\\]]*]\\([^)]*\\)|<img\\s[^>]*>)\\s*$")
}
