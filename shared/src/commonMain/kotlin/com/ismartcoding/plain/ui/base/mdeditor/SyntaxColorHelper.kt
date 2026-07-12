package com.ismartcoding.plain.ui.base.mdeditor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object SyntaxColors {
    val keyword = Color(0xFF2F6F9F)
    val attr = Color(0xFF4F9FCF)
    val attrValue = Color(0xFFD44950)
    val comment = Color(0xFF999999)
    val string = Color(0xFFD44950)
    val number = Color(0xFFFF6600)
    val variable = Color(0xFF009688)
}

@Composable
internal fun matchPatternHighlights(
    pattern: Regex,
    textToHighlight: CharSequence,
    firstColoredIndex: Int,
): List<HighlightInfo> {
    val color: Color = when (pattern) {
        Patterns.HTML_TAGS, Patterns.GENERAL_KEYWORDS, Patterns.SQL_KEYWORDS,
        Patterns.PY_KEYWORDS, Patterns.LUA_KEYWORDS -> SyntaxColors.keyword
        Patterns.HTML_ATTRS, Patterns.CSS_ATTRS, Patterns.LINK -> SyntaxColors.attr
        Patterns.CSS_ATTR_VALUE -> SyntaxColors.attrValue
        Patterns.XML_COMMENTS, Patterns.GENERAL_COMMENTS, Patterns.GENERAL_COMMENTS_NO_SLASH -> SyntaxColors.comment
        Patterns.GENERAL_STRINGS -> SyntaxColors.string
        Patterns.NUMBERS, Patterns.SYMBOLS, Patterns.NUMBERS_OR_SYMBOLS -> SyntaxColors.number
        Patterns.PHP_VARIABLES -> SyntaxColors.variable
        else -> MaterialTheme.colorScheme.onSurface
    }

    val highlights = mutableListOf<HighlightInfo>()
    for (match in pattern.findAll(textToHighlight)) {
        highlights.add(
            HighlightInfo(
                color,
                firstColoredIndex + match.range.first,
                firstColoredIndex + match.range.last + 1,
            ),
        )
    }
    return highlights
}
