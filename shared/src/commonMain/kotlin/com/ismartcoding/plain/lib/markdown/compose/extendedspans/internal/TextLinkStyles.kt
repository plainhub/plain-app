package com.ismartcoding.plain.lib.markdown.compose.extendedspans.internal

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles

/**
 * Updates the [TextLinkStyles] with the provided [block].
 */
fun TextLinkStyles.update(block: SpanStyle.() -> SpanStyle): TextLinkStyles {
    return TextLinkStyles(
        style = style?.run(block),
        focusedStyle = focusedStyle?.run(block),
        hoveredStyle = focusedStyle?.run(block),
        pressedStyle = focusedStyle?.run(block),
    )
}