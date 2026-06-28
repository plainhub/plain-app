package com.ismartcoding.plain.lib.markdown.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.ismartcoding.plain.lib.markdown.compose.components.MarkdownComponents
import com.ismartcoding.plain.lib.markdown.compose.components.markdownComponents
import com.ismartcoding.plain.lib.markdown.model.BulletHandler
import com.ismartcoding.plain.lib.markdown.model.DefaultMarkdownAnnotator
import com.ismartcoding.plain.lib.markdown.model.DefaultMarkdownAnnotatorConfig
import com.ismartcoding.plain.lib.markdown.model.DefaultMarkdownExtendedSpans
import com.ismartcoding.plain.lib.markdown.model.DefaultMarkdownInlineContent
import com.ismartcoding.plain.lib.markdown.model.ImageTransformer
import com.ismartcoding.plain.lib.markdown.model.ImageWidth
import com.ismartcoding.plain.lib.markdown.model.MarkdownA11yLabels
import com.ismartcoding.plain.lib.markdown.model.MarkdownAnimations
import com.ismartcoding.plain.lib.markdown.model.MarkdownAnnotator
import com.ismartcoding.plain.lib.markdown.model.MarkdownColors
import com.ismartcoding.plain.lib.markdown.model.MarkdownDimens
import com.ismartcoding.plain.lib.markdown.model.MarkdownExtendedSpans
import com.ismartcoding.plain.lib.markdown.model.MarkdownInlineContent
import com.ismartcoding.plain.lib.markdown.model.MarkdownPadding
import com.ismartcoding.plain.lib.markdown.model.MarkdownTypography
import com.ismartcoding.plain.lib.markdown.model.ReferenceLinkHandler

/**
 * The CompositionLocal to provide functionality related to transforming the bullet of an ordered list
 */
val LocalBulletListHandler = staticCompositionLocalOf {
    return@staticCompositionLocalOf BulletHandler { _, _, _, _, _ -> "• " }
}

/**
 * The CompositionLocal to provide functionality related to transforming the bullet of an ordered list
 */
val LocalOrderedListHandler = staticCompositionLocalOf {
    return@staticCompositionLocalOf BulletHandler { _, _, index, listNumber, _ -> "${listNumber + index}. " }
}

/**
 * Local [ReferenceLinkHandler] provider
 */
val LocalReferenceLinkHandler = staticCompositionLocalOf<ReferenceLinkHandler> {
    error("CompositionLocal ReferenceLinkHandler not present")
}

/**
 * Local [MarkdownColors] provider
 */
val LocalMarkdownColors = compositionLocalOf<MarkdownColors> {
    error("No local MarkdownColors")
}

/**
 * Local [MarkdownTypography] provider
 */
val LocalMarkdownTypography = compositionLocalOf<MarkdownTypography> {
    error("No local MarkdownTypography")
}

/**
 * Local [MarkdownPadding] provider
 */
val LocalMarkdownPadding = staticCompositionLocalOf<MarkdownPadding> {
    error("No local Padding")
}

/**
 * Local [MarkdownDimens] provider
 */
val LocalMarkdownDimens = compositionLocalOf<MarkdownDimens> {
    error("No local MarkdownDimens")
}

/**
 * Local [ImageTransformer] provider
 */
val LocalImageTransformer = staticCompositionLocalOf<ImageTransformer> {
    error("No local ImageTransformer")
}

/**
 * Local [MarkdownInlineContent] provider
 */
val LocalMarkdownInlineContent = staticCompositionLocalOf<MarkdownInlineContent> {
    return@staticCompositionLocalOf DefaultMarkdownInlineContent(mapOf())
}

/**
 * Local [ImageWidth] provider
 */
val LocalImageWidth = staticCompositionLocalOf<ImageWidth> {
    return@staticCompositionLocalOf ImageWidth.IMAGE_WIDTH
}

/**
 * Local [MarkdownAnnotator] provider
 */
val LocalMarkdownAnnotator = compositionLocalOf<MarkdownAnnotator> {
    return@compositionLocalOf DefaultMarkdownAnnotator(null, DefaultMarkdownAnnotatorConfig())
}

/**
 * Local [MarkdownExtendedSpans] provider
 */
val LocalMarkdownExtendedSpans = compositionLocalOf<MarkdownExtendedSpans> {
    return@compositionLocalOf DefaultMarkdownExtendedSpans(null)
}

/**
 * Local [MarkdownComponents] provider
 */
val LocalMarkdownComponents = compositionLocalOf<MarkdownComponents> {
    return@compositionLocalOf markdownComponents()
}

/**
 * Local [MarkdownAnimations] provider
 */
val LocalMarkdownAnimations = compositionLocalOf<MarkdownAnimations> {
    error("No local MarkdownAnimations")
}

/**
 * Local [MarkdownA11yLabels] provider — override to localize accessibility labels.
 */
val LocalMarkdownA11yLabels = staticCompositionLocalOf { MarkdownA11yLabels() }
