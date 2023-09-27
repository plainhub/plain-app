package com.ismartcoding.plain.ui.views.texteditor

import com.ismartcoding.plain.MainApp
import java.util.regex.Pattern

class HighlightDriver(
    private val colorProvider: HighlightColorProvider,
    private val fileExtension: String,
) {
    fun highlightText(
        textToHighlight: CharSequence,
        firstColoredIndex: Int,
    ): MutableList<HighlightInfo> {
        val highlights = mutableListOf<HighlightInfo>()

        if (fileExtension.contains(HtmlExtension) ||
            fileExtension.contains(XmlExtension)
        ) {
            highlights.addAll(color(Patterns.HTML_TAGS, textToHighlight, firstColoredIndex))
            highlights.addAll(color(Patterns.HTML_ATTRS, textToHighlight, firstColoredIndex))
            highlights.addAll(color(Patterns.GENERAL_STRINGS, textToHighlight, firstColoredIndex))
            highlights.addAll(color(Patterns.XML_COMMENTS, textToHighlight, firstColoredIndex))
        } else if (fileExtension.contains(CssExtension)) {
            highlights.addAll(color(Patterns.CSS_ATTRS, textToHighlight, firstColoredIndex))
            highlights.addAll(color(Patterns.CSS_ATTR_VALUE, textToHighlight, firstColoredIndex))
            highlights.addAll(color(Patterns.SYMBOLS, textToHighlight, firstColoredIndex))
            highlights.addAll(color(Patterns.GENERAL_COMMENTS, textToHighlight, firstColoredIndex))
        } else if (listOf<String>(*MimeTypes.MIME_CODE).contains(fileExtension)) {
            highlights.addAll(
                when (fileExtension) {
                    LuaExtension -> color(Patterns.LUA_KEYWORDS, textToHighlight, firstColoredIndex)
                    PyExtension -> color(Patterns.PY_KEYWORDS, textToHighlight, firstColoredIndex)
                    else -> color(Patterns.GENERAL_KEYWORDS, textToHighlight, firstColoredIndex)
                },
            )
            highlights.addAll(color(Patterns.NUMBERS_OR_SYMBOLS, textToHighlight, firstColoredIndex))
            highlights.addAll(color(Patterns.GENERAL_STRINGS, textToHighlight, firstColoredIndex))
            highlights.addAll(color(Patterns.GENERAL_COMMENTS, textToHighlight, firstColoredIndex))
            if (fileExtension == PhpExtension) {
                highlights.addAll(color(Patterns.PHP_VARIABLES, textToHighlight, firstColoredIndex))
            }
        } else if (listOf<String>(*MimeTypes.MIME_SQL).contains(fileExtension)) {
            highlights.addAll(color(Patterns.SYMBOLS, textToHighlight, firstColoredIndex))
            highlights.addAll(color(Patterns.GENERAL_STRINGS, textToHighlight, firstColoredIndex))
            highlights.addAll(color(Patterns.SQL_KEYWORDS, textToHighlight, firstColoredIndex))
        } else {
            if (!listOf(*MimeTypes.MIME_MARKDOWN).contains(fileExtension)) {
                highlights.addAll(color(Patterns.GENERAL_KEYWORDS, textToHighlight, firstColoredIndex))
            }
            highlights.addAll(color(Patterns.NUMBERS_OR_SYMBOLS, textToHighlight, firstColoredIndex))
            highlights.addAll(color(Patterns.GENERAL_STRINGS, textToHighlight, firstColoredIndex))
            if (fileExtension == "prop" || fileExtension.contains("conf") ||
                listOf(*MimeTypes.MIME_MARKDOWN).contains(fileExtension)
            ) {
                highlights.addAll(color(Patterns.GENERAL_COMMENTS_NO_SLASH, textToHighlight, firstColoredIndex))
            } else {
                highlights.addAll(color(Patterns.GENERAL_COMMENTS, textToHighlight, firstColoredIndex))
            }

            if (listOf(*MimeTypes.MIME_MARKDOWN).contains(fileExtension)) {
                highlights.addAll(color(Patterns.LINK, textToHighlight, firstColoredIndex))
            }
        }

        return highlights
    }

    private fun color(
        pattern: Pattern,
        textToHighlight: CharSequence,
        firstColoredIndex: Int,
    ): List<HighlightInfo> {
        var color = 0
        when (pattern) {
            Patterns.HTML_TAGS, Patterns.GENERAL_KEYWORDS, Patterns.SQL_KEYWORDS, Patterns.PY_KEYWORDS, Patterns.LUA_KEYWORDS -> {
                color = colorProvider.keywordColor
            }
            Patterns.HTML_ATTRS, Patterns.CSS_ATTRS, Patterns.LINK -> {
                color = colorProvider.attrColor
            }
            Patterns.CSS_ATTR_VALUE -> {
                color = colorProvider.attrValueColor
            }
            Patterns.XML_COMMENTS, Patterns.GENERAL_COMMENTS, Patterns.GENERAL_COMMENTS_NO_SLASH -> {
                color = colorProvider.commentColor
            }
            Patterns.GENERAL_STRINGS -> {
                color = colorProvider.stringColor
            }
            Patterns.NUMBERS, Patterns.SYMBOLS, Patterns.NUMBERS_OR_SYMBOLS -> {
                color = colorProvider.numberColor
            }
            Patterns.PHP_VARIABLES -> {
                color = colorProvider.variableColor
            }
        }

        val m = pattern.matcher(textToHighlight)
        val highlights = mutableListOf<HighlightInfo>()

        while (m.find()) {
            highlights.add(
                HighlightInfo(
                    MainApp.instance.getColor(color),
                    firstColoredIndex + m.start(),
                    firstColoredIndex + m.end(),
                ),
            )
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
    val color: Int,
    val start: Int,
    val end: Int,
)
