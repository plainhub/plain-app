package com.ismartcoding.plain.ui.views.texteditor

import com.ismartcoding.plain.R

interface HighlightColorProvider {
    val keywordColor: Int

    val attrColor: Int

    val attrValueColor: Int

    val commentColor: Int

    val stringColor: Int

    val numberColor: Int

    val variableColor: Int
}

class AndroidHighlightColorProvider : HighlightColorProvider {
    override val keywordColor: Int
        get() = R.color.syntax_keyword

    override val attrColor: Int
        get() = R.color.syntax_attr

    override val attrValueColor: Int
        get() = R.color.syntax_attr_value

    override val commentColor: Int
        get() = R.color.syntax_comment

    override val stringColor: Int
        get() = R.color.syntax_string

    override val numberColor: Int
        get() = R.color.syntax_number

    override val variableColor: Int
        get() = R.color.syntax_variable
}
