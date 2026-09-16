package com.ismartcoding.plain.ui.components.codeeditor

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import com.ismartcoding.plain.lib.codeeditor.TokenKind

/**
 * Syntax palette. Light theme is the first-class citizen (e-ink friendly: high contrast on
 * white); dark values track the widely understood editor conventions at soft luminance.
 */
data class EditorSyntaxColors(
    val keyword: Color,
    val type: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val annotation: Color,
    val function: Color,
    val tag: Color,
    val attribute: Color,
    val constant: Color,
    val variable: Color,
) {
    fun forKind(kind: TokenKind): Color? = when (kind) {
        TokenKind.PLAIN -> null
        TokenKind.KEYWORD -> keyword
        TokenKind.TYPE -> type
        TokenKind.STRING -> string
        TokenKind.NUMBER -> number
        TokenKind.COMMENT -> comment
        TokenKind.ANNOTATION -> annotation
        TokenKind.FUNCTION -> function
        TokenKind.OPERATOR -> null
        TokenKind.TAG -> tag
        TokenKind.ATTRIBUTE -> attribute
        TokenKind.CONSTANT -> constant
        TokenKind.VARIABLE -> variable
    }
}

val LightSyntaxColors = EditorSyntaxColors(
    keyword = Color(0xFF8B65B3),
    type = Color(0xFF4682B4),
    string = Color(0xFF59996A),
    number = Color(0xFFB28455),
    comment = Color(0xFF99999F),
    annotation = Color(0xFFA67FAB),
    function = Color(0xFF7187C4),
    tag = Color(0xFFC48282),
    attribute = Color(0xFF8F8F52),
    constant = Color(0xFF6FA0CF),
    variable = Color(0xFF7A96A8),
)

val DarkSyntaxColors = EditorSyntaxColors(
    keyword = Color(0xFFC586C0),
    type = Color(0xFF4EC9B0),
    string = Color(0xFFCE9178),
    number = Color(0xFFB5CEA8),
    comment = Color(0xFF8CA0AA),
    annotation = Color(0xFFC586C0),
    function = Color(0xFFDCDCAA),
    tag = Color(0xFF6FB3D2),
    attribute = Color(0xFF9CDCFE),
    constant = Color(0xFF8CD9FF),
    variable = Color(0xFF9CDCFE),
)

val LocalEditorSyntaxColors = staticCompositionLocalOf { LightSyntaxColors }

/** Non-composable span builder so row text can be constructed outside composition. */
fun syntaxSpanStyleFor(colors: EditorSyntaxColors, kind: TokenKind): SpanStyle? {
    val color = colors.forKind(kind) ?: return null
    return if (kind == TokenKind.COMMENT) {
        SpanStyle(color = color, fontStyle = FontStyle.Italic)
    } else {
        SpanStyle(color = color)
    }
}
