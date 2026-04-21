package com.ismartcoding.lib.kgraphql.schema.model.ast

import kotlin.math.floor

/**
 * A representation of source input to GraphQL.
 * [name] and [locationOffset] are optional. They are useful for clients who
 * store GraphQL documents in source files; for example, if the GraphQL input
 * starts at line 40 in a file named Foo.graphql, it might be useful for [name] to
 * be "Foo.graphql" and location to be `{ line: 40, column: 0 }`.
 * line and column in [locationOffset] are 1-indexed
 */
data class Source(
    val body: String,
    val name: String = "GraphQL request",
    val locationOffset: LocationSource = LocationSource(
        1,
        1
    )
) {
    fun print(location: LocationSource): String {
        val firstLineColumnOffset = locationOffset.column - 1
        val body = whitespace(firstLineColumnOffset) + body

        val lineIndex = location.line - 1
        val lineOffset = locationOffset.line - 1
        val lineNum = location.line + lineOffset

        val columnOffset = if (location.line == 1) firstLineColumnOffset else 0
        val columnNum = location.column + columnOffset
        val locationStr = "$name:$lineNum:$columnNum\n"

        val lines = body.split("\\r\\n|[\\n\\r]".toRegex())
        val locationLine = lines[lineIndex]

        // Special case for minified documents
        if (locationLine.length > 120) {
            val subLineIndex = floor(columnNum.toDouble() / 80).toInt()
            val subLineColumnNum = columnNum % 80
            val subLines = locationLine.chunked(80)

            return (
                locationStr +
                printPrefixedLines(
                    listOf(
                        listOf("$lineNum", subLines[0]),
                        subLines.slice(1..(subLineIndex + 1)).map { listOf("", it) }.flatten(),
                        listOf(" ", whitespace(subLineColumnNum - 1) + "^"),
                        listOf("", subLines[subLineIndex + 1])
                    )
                )
            )
        }

        return (
            locationStr +
            printPrefixedLines(
                listOf(
                    // Lines specified like this: ["prefix", "string"],
                    listOf("${lineNum - 1}", lines.getOrNull(lineIndex - 1)),
                    listOf("$lineNum", locationLine),
                    listOf("", whitespace(columnNum - 1) + "^"),
                    listOf("${lineNum + 1}", lines.getOrNull(lineIndex + 1))
                )
            )
        )
    }

    private fun printPrefixedLines(lines: List<List<String?>>): String {
        val existingLines = lines.filter { it.getOrNull(1) != null }

        val padLen = existingLines.map { it[0]?.length?:error("item line length is null") }.maxOrNull()?:error("line is null")
        return existingLines.joinToString("\n") {
            lpad(padLen, it[0]!!) + if (it.getOrNull(1).isNullOrBlank()) " |" else " | " + it[1]
        }
    }

    private fun whitespace(len: Int): String = (1..len).joinToString("") { " " }

    private fun lpad(len: Int, str: String) = whitespace(len - str.length) + str
    data class LocationSource(val line: Int, val column: Int)
}
