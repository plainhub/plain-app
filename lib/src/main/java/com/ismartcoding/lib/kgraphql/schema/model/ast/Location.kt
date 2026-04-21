package com.ismartcoding.lib.kgraphql.schema.model.ast

/**
 * Contains a range of UTF-8 character offsets and token references that
 * identify the region of the source from which the AST derived.
 */
class Location(
    /**
     * The character offset at which this Node begins.
     */
    val start: Int,

    /**
     * The character offset at which this Node ends.
     */
    val end: Int,

    /**
     * The Token at which this Node begins.
     */
    val startToken: Token,

    /**
     * The Token at which this Node ends.
     */
    val endToken: Token,

    /**
     * The Source document the AST represents.
     */
    val source: Source
) {

    constructor(startToken: Token, endToken: Token, source: Source) : this(
        start = startToken.start,
        end = endToken.end,
        startToken = startToken,
        endToken = endToken,
        source = source
    )

    fun printLocation(): String = source.print(
        getLocation(
            source,
            start
        )
    )

    companion object {
        fun getLocation(source: Source, position: Int): Source.LocationSource {
            val lineRegexp = "\\r\\n|[\\n\\r]".toRegex()
            var line = 1
            var column = position + 1

            lineRegexp.findAll(source.body).toList().map {
                if (it.range.first < position) {
                    line += 1
                    column = position + 1 - (it.range.first + it.value.length)
                }
            }

            return Source.LocationSource(line, column)
        }
    }
}
