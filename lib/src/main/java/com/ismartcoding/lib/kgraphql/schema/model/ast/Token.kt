package com.ismartcoding.lib.kgraphql.schema.model.ast

/**
 * Represents a range of characters represented by a lexical token
 * within a Source.
 */
data class Token(
    /**
     * The kind of Token.
     */
    val kind: TokenKindEnum,

    /**
     * The character offset at which this Node begins.
     */
    val start: Int = 0,

    /**
     * The character offset at which this Node ends.
     */
    val end: Int = 0,

    /**
     * The 1-indexed line number on which this Token appears.
     */
    val line: Int = 0,

    /**
     * The 1-indexed column number at which this Token begins.
     */
    val column: Int = 0,

    /**
     * For non-punctuation tokens, represents the interpreted value of the token.
     */
    val value: String? = null,

    /**
     * Tokens exist as nodes in a double-linked-list amongst all tokens
     * including ignored tokens. <SOF> is always the first node and <EOF>
     * the last.
     */
    var prev: Token? = null,
    var next: Token? = null
) {
    override fun toString() = "[kind: $kind, start: $start, end: $end, line: $line, column: $column, value: $value]"

}
