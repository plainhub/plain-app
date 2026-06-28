package com.ismartcoding.plain.lib.markdown.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.lib.markdown.compose.Markdown

@Immutable
interface MarkdownColors {
    /** Represents the color used for the text of this [Markdown] component. */
    val text: Color

    /** Represents the color used for the background of code. */
    val codeBackground: Color

    /** Represents the color used for the inline background of code. */
    val inlineCodeBackground: Color

    /** Represents the color used for the color of dividers. */
    val dividerColor: Color

    /** Represents the color used for the background of tables. */
    val tableBackground: Color
}

@Immutable
data class DefaultMarkdownColors(
    override val text: Color,
    override val codeBackground: Color,
    override val inlineCodeBackground: Color,
    override val dividerColor: Color,
    override val tableBackground: Color,
) : MarkdownColors