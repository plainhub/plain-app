package com.ismartcoding.plain.lib.markdown.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
@Immutable
interface MarkdownAnnotatorConfig {
    /** Defines if a EOL should be treated as a new line */
    val eolAsNewLine: Boolean

    /**
     * If `true`, inline images that are taller than the surrounding line
     * height (by [BLOCK_FALLBACK_LINE_MULTIPLIER]) are promoted to block
     * rendering below the text, instead of being drawn as an inline
     * placeholder. This avoids overlap with preceding lines on platforms
     * where Compose's text engine does not grow line metrics to fit tall
     * placeholders.
     */
    val inlineImageAsBlock: Boolean
        get() = true

    /**
     * If `true`, hovering an image surfaces its alt text in a small popup
     * (web-style tooltip). On touch-only platforms a long-press also
     * triggers it via [androidx.compose.foundation.hoverable]'s interaction
     * source.
     */
    val showImageAltTooltip: Boolean
        get() = false

    /**
     * Hover dwell time in milliseconds before the alt-text tooltip appears.
     * Only honored when [showImageAltTooltip] is `true`.
     */
    val imageAltTooltipHoverDelayMs: Long
        get() = 2_000L

    companion object {
        /**
         * Multiplier of the surrounding text's line height above which an
         * inline image is promoted to a block element when
         * [inlineImageAsBlock] is enabled.
         */
        const val BLOCK_FALLBACK_LINE_MULTIPLIER = 1.5f
    }
}

@Stable
@Immutable
class DefaultMarkdownAnnotatorConfig(
    override val eolAsNewLine: Boolean = false,
    override val inlineImageAsBlock: Boolean = true,
    override val showImageAltTooltip: Boolean = false,
    override val imageAltTooltipHoverDelayMs: Long = 2_000L,
) : MarkdownAnnotatorConfig {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultMarkdownAnnotatorConfig

        if (eolAsNewLine != other.eolAsNewLine) return false
        if (inlineImageAsBlock != other.inlineImageAsBlock) return false
        if (showImageAltTooltip != other.showImageAltTooltip) return false
        if (imageAltTooltipHoverDelayMs != other.imageAltTooltipHoverDelayMs) return false
        return true
    }

    override fun hashCode(): Int {
        var result = eolAsNewLine.hashCode()
        result = 31 * result + inlineImageAsBlock.hashCode()
        result = 31 * result + showImageAltTooltip.hashCode()
        result = 31 * result + imageAltTooltipHoverDelayMs.hashCode()
        return result
    }
}

fun markdownAnnotatorConfig(
    eolAsNewLine: Boolean = false,
    inlineImageAsBlock: Boolean = true,
    showImageAltTooltip: Boolean = false,
    imageAltTooltipHoverDelayMs: Long = 2_000L,
): MarkdownAnnotatorConfig = DefaultMarkdownAnnotatorConfig(
    eolAsNewLine = eolAsNewLine,
    inlineImageAsBlock = inlineImageAsBlock,
    showImageAltTooltip = showImageAltTooltip,
    imageAltTooltipHoverDelayMs = imageAltTooltipHoverDelayMs,
)
