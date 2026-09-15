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
    keyword = Color(0xFF7A00B8),
    type = Color(0xFF0B7285),
    string = Color(0xFF0B6E3A),
    number = Color(0xFF9A5B00),
    comment = Color(0xFF5F6B76),
    annotation = Color(0xFF7A00B8),
    function = Color(0xFF8A5A00),
    tag = Color(0xFF9A2B2B),
    attribute = Color(0xFFB04000),
    constant = Color(0xFF1F3A93),
    variable = Color(0xFF1F3A93),
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
