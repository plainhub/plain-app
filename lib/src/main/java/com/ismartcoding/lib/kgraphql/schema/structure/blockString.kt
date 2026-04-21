package com.ismartcoding.lib.kgraphql.schema.structure

/**
 * Produces the value of a block string from its parsed raw value, similar to
 * CoffeeScript's block string, Python's docstring trim or Ruby's strip_heredoc.
 *
 * This implements the GraphQL spec's BlockStringValue() static algorithm.
 */
internal fun dedentBlockStringValue(rawString: String): String {
    // Expand a block string's raw value into independent lines.
    var lines = rawString.split(Regex("\\r\\n|[\\n\\r]")).toMutableList()

    // Remove common indentation from all lines but first.
    val commonIndent = getBlockStringIndentation(lines)

    if (commonIndent != 0) {
        for (index in lines.drop(1).indices) {
            val line = lines[index + 1]
            val toRemove = if (line.length <= commonIndent - 1) line.length else commonIndent
            lines[index + 1] = line.removeRange(0, toRemove)
        }
    }

    // Remove leading and trailing blank lines.
    while (lines.size > 0 && isBlank(lines[0])) {
        lines = lines.drop(1).toMutableList()
    }
    while (lines.size > 0 && isBlank(lines[lines.size - 1])) {
        lines = lines.dropLast(1).toMutableList()
    }

    // Return a string of the lines joined with U+000A.
    return lines.joinToString("\n")
}


internal fun getBlockStringIndentation(lines: List<String>): Int {
    var commonIndent: Int? = null

    for (line in lines.drop(1)) {
        val indent = leadingWhitespace(line)
        if (indent == line.length) {
            continue
        }

        if (commonIndent == null || indent < commonIndent) {
            commonIndent = indent
            if (commonIndent == 0) {
                break
            }
        }
    }

    return commonIndent ?: 0
}

internal fun leadingWhitespace(str: String): Int {
    var i = 0
    while (i < str.length && (str[i] == ' ' || str[i] == '\t')) {
        i++
    }
    return i
}

internal fun isBlank(str: String) = leadingWhitespace(str) == str.length

/**
 * Print a block string in the indented block form by adding a leading and
 * trailing blank line. However, if a block string starts with whitespace and is
 * a single-line, adding a leading blank line would strip that whitespace.
 */
internal fun printBlockString(
    value: String,
    indentation: String? = "",
    preferMultipleLines: Boolean = false
): String {
    val isSingleLine = value.indexOf('\n') == -1
    val hasLeadingSpace = value[0] == ' ' || value[0] == '\t'
    val hasTrailingQuote = value[value.length - 1] == '"'
    val printAsMultipleLines = !isSingleLine || hasTrailingQuote || preferMultipleLines

    var result = ""
    // Format a multi-line block quote to account for leading space.
    if (printAsMultipleLines && !(isSingleLine && hasLeadingSpace)) {
        result += "\n$indentation"
    }
    result += indentation?.let { value.replace(Regex("\n"), "\n$indentation") } ?: value
    if (printAsMultipleLines) {
        result += '\n'
    }

    return "\"\"\"" + result.replace(Regex("\"\"\""), "\\\"\"\"") + "\"\"\""
}
