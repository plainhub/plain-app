package com.ismartcoding.lib.kgraphql.request

import com.ismartcoding.lib.kgraphql.schema.model.ast.Source
import com.ismartcoding.lib.kgraphql.schema.model.ast.Token
import com.ismartcoding.lib.kgraphql.schema.model.ast.TokenKindEnum
import com.ismartcoding.lib.kgraphql.schema.model.ast.TokenKindEnum.*
import com.ismartcoding.lib.kgraphql.schema.structure.dedentBlockStringValue

data class Lexer(
    val source: Source,
    val options: Any? = null,

    /** The previously focused non-ignored token. */
    var lastToken: Token = Token(SOF),

    /** The currently focused non-ignored token. */
    var token: Token = lastToken,

    /** The (1-indexed) line containing the current token. */
    var line: Int = 1,

    /** The character offset at which the current line begins.*/
    var lineStart: Int = 0
) {
    constructor(source: String): this (Source(source))


    /**
     * Advances the token stream to the next non-ignored token.
     */
    fun advance(): Token {
        lastToken = token
        token = lookahead()
        return token
    }

    /**
     * Looks ahead and returns the next non-ignored token, but does not change
     * the Lexer's state.
     */
    fun lookahead(): Token {
        var token = this.token
        if (token.kind != EOF) {
            do {
                token = if (token.next == null) {
                    val nextToken = readToken(token)
                    token.next = nextToken
                    nextToken
                } else token.next!!
            } while (token.kind === COMMENT)
        }
        return token
    }

    /**
     * Gets the next token from the source starting at the given position.
     *
     * This skips over whitespace until it finds the next lexable token, then lexes
     * punctuators immediately or calls the appropriate helper function for more
     * complicated tokens.
     */
    private fun readToken(prev: Token): Token {
        val source = source
        val body = source.body
        val bodyLength = body.length

        val pos = positionAfterWhitespace(body, prev.end)
        val col = 1 + pos - lineStart

        fun tok(
            kind: TokenKindEnum,
            start: Int = pos,
            end: Int = pos + 1
        ) = Token(
            kind = kind,
            start = start,
            end = end,
            line = line,
            column = col,
            prev = prev
        )

        if (pos >= bodyLength) {
            return tok(EOF, bodyLength, bodyLength)
        }


        val fail = { code: Int -> throw syntaxError(
            source,
            pos,
            unexpectedCharacterMessage(code)
        )
        }

        return when (val code = body[pos].toInt()) {
            // !
            33 -> tok(BANG)
            // #
            35 -> readComment(pos, col, prev)
            // $
            36 -> tok(DOLLAR)
            // &
            38 -> tok(AMP)
            // (
            40 -> tok(PAREN_L)
            // )
            41 -> tok(PAREN_R)
            // .
            46 -> if (body.getOrNull(pos + 1)?.toInt() == 46 && body.getOrNull(pos + 2)?.toInt() == 46) {
                tok(SPREAD, start = pos, end = pos + 3)
            } else fail(code)
            // :
            58 -> tok(COLON)
            // =
            61 -> tok(EQUALS)
            // @
            64 -> tok(AT)
            // [
            91 -> tok(BRACKET_L)
            // ]
            93 -> tok(BRACKET_R)
            // {
            123 -> tok(BRACE_L)
            // |
            124 -> tok(PIPE)
            // }
            125 -> tok(BRACE_R)
            // A-Z _ a-z
            in (65..90), 95, in (97..122) -> readName(pos, col, prev)
            // - 0-9
            45, in (48..57) -> readNumber(pos, code, col, prev)
            // "
            34 -> {
                if (body.getOrNull(pos + 1)?.toInt() == 34 && body.getOrNull(pos + 2)?.toInt() == 34)
                    readBlockString(pos, col, prev)
                else readString(pos, col, prev)
            }
            else -> fail(code)
        }
    }

    /**
     * Reads from body starting at startPosition until it finds a non-whitespace
     * character, then returns the position of that character for lexing.
     */
    private fun positionAfterWhitespace(body: String, startPosition: Int): Int {
        val bodyLength = body.length
        var position = startPosition
        while (position < bodyLength) {
            val code = body[position].toInt()
            // tab | space | comma | BOM
            if (code == 9 || code == 32 || code == 44 || code == 0xfeff) {
                ++position;
            } else if (code == 10) {
                // new line
                ++position
                ++line
                lineStart = position
            } else if (code == 13) {
                // carriage return
                if (body[position + 1].toInt() == 10) {
                    position += 2
                } else {
                    ++position
                }
                ++line
                lineStart = position
            } else {
                break
            }
        }
        return position
    }

    /**
     * Reads a comment token from the source file.
     *
     * #[\u0009\u0020-\uFFFF]*
     */
    private fun readComment(start: Int, col: Int, prev: Token): Token {
        var code: Char?
        var position = start

        do {
            code = source.body.getOrNull(++position)
        } while (
        // SourceCharacter but not LineTerminator
            code != null && (code > '\u001f' || code == '\u0009')
        )

        return Token(
            kind = COMMENT,
            start = start,
            end = position,
            line = line,
            column = col,
            prev = prev,
            value = source.body.substring(start + 1, position)
        )
    }

    /**
     * Reads an alphanumeric + underscore name from the source.
     *
     * [_A-Za-z][_0-9A-Za-z]*
     */
    private fun readName(start: Int, col: Int, prev: Token): Token {
        val body = source.body
        val bodyLength = body.length
        var position = start
        var code = body.getOrNull(position + 1)?.toInt()

        while (code != null && (code == 95 || // -
                    (code in 48..57) || // 0-9
                    (code in 65..90) || // A-Z
                    (code in 97..122)   // a-z
                    )
        ) {
            code = body.getOrNull(++position + 1)?.toInt()
        }

        return Token(
            kind = NAME,
            start = start,
            end = ++position,
            line = line,
            column = col,
            prev = prev,
            value = body.substring(start, position)
        )
    }

    /**
     * Reads a number token from the source file, either a float
     * or an int depending on whether a decimal point appears.
     *
     * Int:   -?(0|[1-9][0-9]*)
     * Float: -?(0|[1-9][0-9]*)(\.[0-9]+)?((E|e)(+|-)?[0-9]+)?
     */
    private fun readNumber(start: Int, firstCode: Int, col: Int, prev: Token): Token {
        val body = source.body
        var code: Int? = firstCode
        var position = start
        var isFloat = false

        if (code == 45) {
            // -
            code = body.getOrNull(++position)?.toInt()
        }

        if (code == 48) {
            // 0
            code = body.getOrNull(++position)?.toInt()
            if (code in 48..57) {
                throw syntaxError(
                    source,
                    position,
                    "Invalid number, unexpected digit after 0: ${printCharCode(code)}."
                )
            }
        } else {
            position = readDigits(position, code)
            code = body.getOrNull(position)?.toInt()
        }

        if (code == 46) {
            // .
            isFloat = true

            code = body.getOrNull(++position)?.toInt()
            position = readDigits(position, code)
            code = body.getOrNull(position)?.toInt()
        }

        if (code == 69 || code == 101) {
            // E e
            isFloat = true

            code = body.getOrNull(++position)?.toInt()
            if (code == 43 || code == 45) {
                // + -
                code = body.getOrNull(++position)?.toInt()
            }
            position = readDigits(position, code)
            code = body.getOrNull(position)?.toInt()
        }

        // Numbers cannot be followed by . or NameStart
        if (code == 46 || isNameStart(code)) {
            throw syntaxError(
                source,
                position,
                "Invalid number, expected digit but got: ${printCharCode(code)}."
            )
        }

        return Token(
            kind = if (isFloat) FLOAT else INT,
            start = start,
            end = position,
            line = line,
            column = col,
            prev = prev,
            value = body.substring(start, position)
        )
    }

    /**
     * Returns the new position in the source after reading digits.
     */
    private fun readDigits(start: Int, firstCode: Int?): Int {
        val body = source.body
        var position = start
        var code = firstCode
        if (code in 48..57) {
            // 0 - 9
            do {
                code = body.getOrNull(++position)?.toInt()
            } while (code in 48..57) // 0 - 9
            return position
        }
        throw syntaxError(
            source,
            position,
            "Invalid number, expected digit but got: ${printCharCode(code)}."
        )
    }

    /**
     * Reads a string token from the source file.
     *
     * "([^"\\\u000A\u000D]|(\\(u[0-9a-fA-F]{4}|["\\/bfnrt])))*"
     */
    private fun readString(start: Int, col: Int, prev: Token): Token {
        val body = source.body
        var position = start + 1
        var chunkStart = position
        var code: Int
        var value = ""

        while (position < body.length) {
            code = body.getOrNull(position)?.toInt() ?: break
            // not LineTerminator
            if (code == 0x00a || code == 0x00d) break

            // Closing Quote (")
            if (code == 34) {
                value += body.substring(chunkStart, position)
                return Token(
                    kind = STRING,
                    start = start,
                    end = position + 1,
                    line = line,
                    column = col,
                    prev = prev,
                    value = value
                )
            }

            // SourceCharacter
            if (code < 0x0020 && code != 0x0009) {
                throw syntaxError(
                    source,
                    position,
                    "Invalid character within String: ${printCharCode(code)}."
                );
            }

            ++position
            if (code == 92) {
                // \
                value += body.substring(chunkStart, position - 1)
                code = body[position].toInt()
                when (code) {
                    34 -> value += '"'
                    47 -> value += '/'
                    92 -> value += '\\'
                    98 -> value += '\b'
                    102 -> value += 'f'
                    110 -> value += '\n'
                    114 -> value += '\r'
                    116 -> value += '\t'
                    117 -> {
                        // uXXXX
                        val charCode = uniCharCode(
                            body[position + 1].toInt(),
                            body[position + 2].toInt(),
                            body[position + 3].toInt(),
                            body[position + 4].toInt()
                        )
                        if (charCode < 0) {
                            val invalidSequence = body.substring(position + 1, position + 5)
                            throw syntaxError(
                                source,
                                position,
                                "Invalid character escape sequence: \\u${invalidSequence}."
                            )
                        }
                        value += charCode.toChar()
                        position += 4
                    }
                    else -> throw syntaxError(
                        source,
                        position,
                        "Invalid character escape sequence: \\${code.toChar()}."
                    )
                }
                ++position
                chunkStart = position
            }
        }

        throw syntaxError(source, position, "Unterminated string.")
    }

    // _ A-Z a-z
    private fun isNameStart(code: Int?) = (
        code == 95 || (code in 65..90) || (code in 97..122)
    )

    /**
     * Reads a block string token from the source file.
     *
     * """("?"?(\\"""|\\(?!=""")|[^"\\]))*"""
     */
    private fun readBlockString(start: Int, col: Int, prev: Token): Token {
        val body = source.body
        var position = start + 3
        var chunkStart = position
        var code: Int? = 0
        var rawValue = ""

        while (position < body.length && code != null) {
            code = body.getOrNull(position)?.toInt() ?: break
            // Closing Triple-Quote (""")
            if (
                code == 34 &&
                body.getOrNull(position + 1)?.toInt() == 34 &&
                body.getOrNull(position + 2)?.toInt() == 34
            ) {
                rawValue += body.substring(chunkStart, position)
                return Token(
                    kind = BLOCK_STRING,
                    start = start,
                    end = position + 3,
                    line = line,
                    column = col,
                    prev = prev,
                    value = dedentBlockStringValue(rawValue)
                )
            }

            // SourceCharacter
            if (
                code < 0x0020 &&
                code != 0x0009 &&
                code != 0x000a &&
                code != 0x000d
            ) {
                throw syntaxError(
                    source,
                    position,
                    "Invalid character within String: ${printCharCode(code)}."
                )
            }

            if (code == 10) {
                ++position
                ++line
                lineStart = position
            } else if (code == 13) {
                position += if (body.getOrNull(position + 1)?.toInt() == 10) 2 else 1
                ++line
                lineStart = position
            } else if (
                // Escape Triple-Quote (\""")
                code == 92 &&
                body.getOrNull(position + 1)?.toInt() == 34 &&
                body.getOrNull(position + 2)?.toInt() == 34 &&
                body.getOrNull(position + 3)?.toInt() == 34
            ) {
                rawValue += body.substring(chunkStart, position) + "\"\"\""
                position += 4
                chunkStart = position
            } else {
                ++position
            }
        }

        throw syntaxError(source, position, "Unterminated string.")
    }

    /**
     * Converts four hexadecimal chars to the integer that the
     * string represents. For example, uniCharCode('0','0','0','f')
     * will return 15, and uniCharCode('0','0','f','f') returns 255.
     *
     * Returns a negative number on error, if a char was invalid.
     *
     * This is implemented by noting that char2hex() returns -1 on error,
     * which means the result of ORing the char2hex() will also be negative.
     */
    private fun uniCharCode(a: Int, b: Int, c: Int, d: Int) = char2hex(a)
        .shl(12)
        .or(char2hex(b).shl(8))
        .or(char2hex(c).shl(4))
        .or(char2hex(d))

    /**
     * Converts a hex character to its integer value.
     * '0' becomes 0, '9' becomes 9
     * 'A' becomes 10, 'F' becomes 15
     * 'a' becomes 10, 'f' becomes 15
     *
     * Returns -1 on error.
     */
    private fun char2hex(a: Int) = when (a) {
        in 48..57 -> a - 48 // 0-9
        in 65..70 -> a - 55 // A-F
        in 97..102 -> a - 87 // a-f
        else -> -1
    }

    private fun printCharCode(code: Int?) =
        if (code == null) EOF.str
        else StringBuilder().apply {
            append("\"")
            when (val char = code.toChar()) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else ->
                    if (code < 0x007f && char > ' ') {
                        // Refer: https://utf8-chartable.de/unicode-utf8-table.pl?number=128
                        append(char)
                    } else {
                        append("\\u")
                        val hex = Integer.toHexString(code)
                        for (i in hex.length..3) {
                            append('0')
                        }
                        append(hex.uppercase())
                    }
            }
            append("\"")
        }.toString()

    /**
     * Report a message that an unexpected character was encountered.
     */
    private fun unexpectedCharacterMessage(code: Int): String {
        if (code < 0x0020 && code != 0x0009 && code != 0x000a && code != 0x000d) {
            return "Cannot contain the invalid character ${printCharCode(code)}."
        }

        if (code == 39) {
            // '
            return "Unexpected single quote character ('), did you mean to use a double quote (\")?"
        }

        return "Cannot parse the unexpected character ${printCharCode(code)}."
    }
}
