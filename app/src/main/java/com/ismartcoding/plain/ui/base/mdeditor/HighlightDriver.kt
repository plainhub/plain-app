package com.ismartcoding.plain.ui.base.mdeditor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.ismartcoding.plain.R
import java.util.regex.Pattern

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

    @Composable
    private fun color(
        pattern: Pattern,
        textToHighlight: CharSequence,
        firstColoredIndex: Int,
    ): List<HighlightInfo> {
        val keywordColor: Color = colorResource(id = R.color.syntax_keyword)
        val attrColor: Color  = colorResource(id = R.color.syntax_attr)

        val attrValueColor: Color = colorResource(id = R.color.syntax_attr_value)

        val commentColor: Color = colorResource(id = R.color.syntax_comment)

        val stringColor: Color  = colorResource(id = R.color.syntax_string)

        val numberColor: Color = colorResource(id = R.color.syntax_number)

        val variableColor: Color =  colorResource(id = R.color.syntax_variable)
        var color: Color = MaterialTheme.colorScheme.onSurface
        when (pattern) {
            Patterns.HTML_TAGS, Patterns.GENERAL_KEYWORDS, Patterns.SQL_KEYWORDS, Patterns.PY_KEYWORDS, Patterns.LUA_KEYWORDS -> {
                color = keywordColor
            }

            Patterns.HTML_ATTRS, Patterns.CSS_ATTRS, Patterns.LINK -> {
                color = attrColor
            }

            Patterns.CSS_ATTR_VALUE -> {
                color = attrValueColor
            }

            Patterns.XML_COMMENTS, Patterns.GENERAL_COMMENTS, Patterns.GENERAL_COMMENTS_NO_SLASH -> {
                color = commentColor
            }

            Patterns.GENERAL_STRINGS -> {
                color = stringColor
            }

            Patterns.NUMBERS, Patterns.SYMBOLS, Patterns.NUMBERS_OR_SYMBOLS -> {
                color = numberColor
            }

            Patterns.PHP_VARIABLES -> {
                color = variableColor
            }
        }

        val m = pattern.matcher(textToHighlight)
        val highlights = mutableListOf<HighlightInfo>()

        while (m.find()) {
            highlights.add(
                HighlightInfo(
                    color,
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
    val color: Color,
    val start: Int,
    val end: Int,
)
