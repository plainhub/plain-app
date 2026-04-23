package com.ismartcoding.lib.kgraphql.schema.model.ast

/**
 * The enum type representing the token kinds values.
 */
enum class TokenKindEnum(val str: String) {
    SOF("<SOF>"),
    EOF("<EOF>"),
    BANG("!"),
    DOLLAR("$"),
    AMP("&"),
    PAREN_L("("),
    PAREN_R(")"),
    SPREAD("..."),
    COLON(":"),
    EQUALS("="),
    AT("@"),
    BRACKET_L("["),
    BRACKET_R("]"),
    BRACE_L("{"),
    PIPE("|"),
    BRACE_R("}"),
    NAME("Name"),
    INT("Int"),
    FLOAT("Float"),
    STRING("String"),
    BLOCK_STRING("BlockString"),
    COMMENT("Comment");

    val isPunctuatorTokenKind get() = this in listOf(
        BANG,
        DOLLAR,
        AMP,
        PAREN_L,
        PAREN_R,
        SPREAD,
        COLON,
        EQUALS,
        AT,
        BRACKET_L,
        BRACKET_R,
        BRACE_L,
        PIPE,
        BRACE_R
    )

    override fun toString() = str
}
