package com.ismartcoding.plain.ui.base.mdeditor

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

class HighlightDriver(
    private val fileExtension: String,
) {
    @Composable
    fun highlightText(
        textToHighlight: CharSequence,
        firstColoredIndex: Int,
    ): MutableList<HighlightInfo> {
        val highlights = mutableListOf<HighlightInfo>()

        if (fileExtension.contains(HtmlExtension) ||
            fileExtension.contains(XmlExtension)
        ) {
            highlights.addAll(matchPatternHighlights(Patterns.HTML_TAGS, textToHighlight, firstColoredIndex))
            highlights.addAll(matchPatternHighlights(Patterns.HTML_ATTRS, textToHighlight, firstColoredIndex))
            highlights.addAll(matchPatternHighlights(Patterns.GENERAL_STRINGS, textToHighlight, firstColoredIndex))
            highlights.addAll(matchPatternHighlights(Patterns.XML_COMMENTS, textToHighlight, firstColoredIndex))
        } else if (fileExtension.contains(CssExtension)) {
            highlights.addAll(matchPatternHighlights(Patterns.CSS_ATTRS, textToHighlight, firstColoredIndex))
            highlights.addAll(matchPatternHighlights(Patterns.CSS_ATTR_VALUE, textToHighlight, firstColoredIndex))
            highlights.addAll(matchPatternHighlights(Patterns.SYMBOLS, textToHighlight, firstColoredIndex))
            highlights.addAll(matchPatternHighlights(Patterns.GENERAL_COMMENTS, textToHighlight, firstColoredIndex))
        } else if (listOf<String>(*MimeTypes.MIME_CODE).contains(fileExtension)) {
            highlights.addAll(
                when (fileExtension) {
                    LuaExtension -> matchPatternHighlights(Patterns.LUA_KEYWORDS, textToHighlight, firstColoredIndex)
                    PyExtension -> matchPatternHighlights(Patterns.PY_KEYWORDS, textToHighlight, firstColoredIndex)
                    else -> matchPatternHighlights(Patterns.GENERAL_KEYWORDS, textToHighlight, firstColoredIndex)
                },
            )
            highlights.addAll(matchPatternHighlights(Patterns.NUMBERS_OR_SYMBOLS, textToHighlight, firstColoredIndex))
            highlights.addAll(matchPatternHighlights(Patterns.GENERAL_STRINGS, textToHighlight, firstColoredIndex))
            highlights.addAll(matchPatternHighlights(Patterns.GENERAL_COMMENTS, textToHighlight, firstColoredIndex))
            if (fileExtension == PhpExtension) {
                highlights.addAll(matchPatternHighlights(Patterns.PHP_VARIABLES, textToHighlight, firstColoredIndex))
            }
        } else if (listOf<String>(*MimeTypes.MIME_SQL).contains(fileExtension)) {
            highlights.addAll(matchPatternHighlights(Patterns.SYMBOLS, textToHighlight, firstColoredIndex))
            highlights.addAll(matchPatternHighlights(Patterns.GENERAL_STRINGS, textToHighlight, firstColoredIndex))
            highlights.addAll(matchPatternHighlights(Patterns.SQL_KEYWORDS, textToHighlight, firstColoredIndex))
        } else {
            if (!listOf(*MimeTypes.MIME_MARKDOWN).contains(fileExtension)) {
                highlights.addAll(matchPatternHighlights(Patterns.GENERAL_KEYWORDS, textToHighlight, firstColoredIndex))
            }
            highlights.addAll(matchPatternHighlights(Patterns.NUMBERS_OR_SYMBOLS, textToHighlight, firstColoredIndex))
            highlights.addAll(matchPatternHighlights(Patterns.GENERAL_STRINGS, textToHighlight, firstColoredIndex))
            if (fileExtension == "prop" || fileExtension.contains("conf") ||
                listOf(*MimeTypes.MIME_MARKDOWN).contains(fileExtension)
            ) {
                highlights.addAll(matchPatternHighlights(Patterns.GENERAL_COMMENTS_NO_SLASH, textToHighlight, firstColoredIndex))
            } else {
                highlights.addAll(matchPatternHighlights(Patterns.GENERAL_COMMENTS, textToHighlight, firstColoredIndex))
            }

            if (listOf(*MimeTypes.MIME_MARKDOWN).contains(fileExtension)) {
                highlights.addAll(matchPatternHighlights(Patterns.LINK, textToHighlight, firstColoredIndex))
            }
        }

        return highlights
    }

    companion object {
        private const val HtmlExtension = "htm"
        private const val XmlExtension = "xml"
        private const val CssExtension = "css"
        private const val LuaExtension = "lue"
        private const val PyExtension = "py"
        private const val PhpExtension = "php"
    }
}

data class HighlightInfo(
    val color: Color,
    val start: Int,
    val end: Int,
)
