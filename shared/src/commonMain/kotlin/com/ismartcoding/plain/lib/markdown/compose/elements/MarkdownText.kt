package com.ismartcoding.plain.lib.markdown.compose.elements

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.ismartcoding.plain.lib.markdown.annotator.AnnotatorSettings
import com.ismartcoding.plain.lib.markdown.annotator.annotatorSettings
import com.ismartcoding.plain.lib.markdown.annotator.buildMarkdownAnnotatedString
import com.ismartcoding.plain.lib.markdown.compose.LocalImageTransformer
import com.ismartcoding.plain.lib.markdown.compose.LocalImageWidth
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownAnimations
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownAnnotator
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownColors
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownComponents
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownExtendedSpans
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownInlineContent
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownTypography
import com.ismartcoding.plain.lib.markdown.compose.components.MarkdownComponentModel
import com.ismartcoding.plain.lib.markdown.compose.elements.material.MarkdownBasicText
import com.ismartcoding.plain.lib.markdown.compose.extendedspans.ExtendedSpans
import com.ismartcoding.plain.lib.markdown.compose.extendedspans.drawBehind
import com.ismartcoding.plain.lib.markdown.model.ImageTransformer
import com.ismartcoding.plain.lib.markdown.model.ImageWidth
import com.ismartcoding.plain.lib.markdown.model.MarkdownAnnotatorConfig
import com.ismartcoding.plain.lib.markdown.utils.MARKDOWN_TAG_IMAGE_URL
import com.ismartcoding.plain.lib.markdown.utils.MARKDOWN_TAG_MATH
import com.ismartcoding.plain.lib.markdown.utils.toPxOrZero
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType

@Composable
fun MarkdownText(
    content: String,
    node: ASTNode,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalMarkdownTypography.current.text,
) {
    MarkdownText(AnnotatedString(content), node, modifier, style, sourceContent = content)
}

@Composable
fun MarkdownText(
    content: String,
    node: ASTNode,
    style: TextStyle,
    modifier: Modifier = Modifier,
    contentChildType: IElementType? = null,
    annotatorSettings: AnnotatorSettings = annotatorSettings(),
) {
    val childNode = contentChildType?.run(node::findChildOfType) ?: node
    val styledText = buildAnnotatedString {
        pushStyle(style.toSpanStyle())
        buildMarkdownAnnotatedString(
            content = content,
            node = childNode,
            annotatorSettings = annotatorSettings
        )
        pop()
    }

    MarkdownText(content = styledText, node = node, modifier = modifier, style = style, sourceContent = content)
}

@Composable
fun MarkdownText(
    content: AnnotatedString,
    node: ASTNode,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalMarkdownTypography.current.text,
    extendedSpans: ExtendedSpans? = LocalMarkdownExtendedSpans.current.extendedSpans?.invoke(),
    sourceContent: String? = null,
) {
    MarkdownText(
        content = content,
        node = node,
        modifier = modifier,
        style = style,
        onTextLayout = null,
        sourceContent = sourceContent,
        extendedSpans = extendedSpans,
    )
}

@Composable
fun MarkdownText(
    content: AnnotatedString,
    node: ASTNode,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalMarkdownTypography.current.text,
    onTextLayout: ((TextLayoutResult, Color?) -> Unit)?,
    sourceContent: String? = null,
    extendedSpans: ExtendedSpans? = null,
) {
    val baseColor = LocalMarkdownColors.current.text
    val animations = LocalMarkdownAnimations.current
    val transformer = LocalImageTransformer.current
    val inlineContent = LocalMarkdownInlineContent.current
    val inlineImageWidth = LocalImageWidth.current
    val density = LocalDensity.current
    val annotatorConfig = LocalMarkdownAnnotator.current.config

    val layoutResult: MutableState<TextLayoutResult?> = remember { mutableStateOf(null) }
    val containerSize = remember { mutableStateOf(Size.Unspecified) }
    val imageSizeByLink = remember { mutableStateMapOf<String, Size>() }

    // Resolved line height in pixels; used to decide whether an image
    // should be inline or promoted to a block element. Resolves `sp`, `em`,
    // and unspecified units without crashing (`toPx()` only supports `sp`).
    val lineHeightPx = with(density) {
        // `em` is relative to the font size, so resolve that to px first.
        val fontSizePx = toPxOrZero(style.fontSize, relativeToPx = 0f)
        when {
            style.lineHeight.isSpecified -> toPxOrZero(style.lineHeight, relativeToPx = fontSizePx)
            else -> fontSizePx
        }
    }

    val inlineImageAsBlock = annotatorConfig.inlineImageAsBlock
    val imageNodes = remember(node) { collectImageNodes(node) }
    // Wrap the build in derivedStateOf so chatty image-size updates from
    // imageSizeByLink only invalidate downstream when the resolved content
    // or block-range list actually changes.
    val resolved by remember(
        node,
        inlineContent.inlineContent,
        content,
        transformer,
        inlineImageWidth,
        lineHeightPx,
        inlineImageAsBlock,
        imageNodes,
        density,
    ) {
        derivedStateOf {
            // Pass the SnapshotStateMap directly so the snapshot system tracks
            // per-key reads. Only image URLs actually queried by this node
            // invalidate the derived state.
            val blocks = mutableListOf<BlockImageRange>()
            val map = inlineContent.inlineContent +
                buildMathInlineContent(content = content) +
                buildImageInlineContent(
                content = content,
                node = node,
                transformer = transformer,
                density = density,
                containerSize = containerSize.value,
                inlineImageWidth = inlineImageWidth,
                imageSizeByLink = imageSizeByLink,
                lineHeightPx = lineHeightPx,
                inlineImageAsBlock = inlineImageAsBlock,
                imageNodes = imageNodes,
                onBlockImage = { range -> blocks += range },
                imageSizeChanged = { link, size -> imageSizeByLink += (link to size) },
            )
            map to blocks.sortedBy { it.start }
        }
    }
    val resolvedInlineContent = resolved.first
    val blockImageRanges = resolved.second

    val containerModifier: @Composable (Modifier) -> Modifier = { base ->
        // Pin descendants (text + inline link nodes) to source order. Without this,
        // TalkBack reorders interactive link nodes after non-interactive text — see #487.
        base.semantics { isTraversalGroup = true }.onPlaced {
            it.parentLayoutCoordinates?.also { coordinates ->
                containerSize.value = coordinates.size.toSize()
            }
        }
    }

    val textSegment: @Composable (AnnotatedString, Modifier) -> Unit = { segment, segmentModifier ->
        // Apply ExtendedSpans per-segment so the (0..0) marker the extender
        // adds is preserved in each rendered slice and `drawBehind` aligns
        // with the segment's own text layout.
        val extended = if (extendedSpans != null) {
            remember(segment) { extendedSpans.extend(segment) }
        } else segment
        val segmentDrawModifier = if (extendedSpans != null) {
            segmentModifier.drawBehind(extendedSpans)
        } else segmentModifier
        val hasSegmentLinks = segment.getLinkAnnotations(0, segment.length).isNotEmpty()
        val finalModifier = if (hasSegmentLinks) {
            segmentDrawModifier.semantics(mergeDescendants = true) { }
        } else {
            segmentDrawModifier
        }
        MarkdownBasicText(
            text = extended,
            modifier = finalModifier.let { animations.animateTextSize(it) },
            style = style,
            inlineContent = resolvedInlineContent,
            onTextLayout = { result ->
                layoutResult.value = result
                extendedSpans?.onTextLayout(result, baseColor)
                onTextLayout?.invoke(result, baseColor)
            }
        )
    }

    if (blockImageRanges.isEmpty()) {
        val hasLinks = content.getLinkAnnotations(0, content.length).isNotEmpty()
        if (hasLinks) {
            Box(modifier = containerModifier(modifier)) {
                textSegment(content, Modifier)
            }
        } else {
            textSegment(content, modifier.onPlaced {
                it.parentLayoutCoordinates?.also { coordinates ->
                    containerSize.value = coordinates.size.toSize()
                }
            })
        }
    } else {
        val components = LocalMarkdownComponents.current
        val typography = LocalMarkdownTypography.current
        Column(modifier = containerModifier(modifier)) {
            var cursor = 0
            blockImageRanges.forEach { range ->
                if (range.start > cursor) {
                    textSegment(content.subSequence(cursor, range.start), Modifier)
                }
                if (sourceContent != null && range.imageNode != null) {
                    components.image(MarkdownComponentModel(sourceContent, range.imageNode, typography))
                } else {
                    BlockFallbackImage(range.url)
                }
                cursor = range.end
            }
            if (cursor < content.length) {
                textSegment(content.subSequence(cursor, content.length), Modifier)
            }
        }
    }
}

internal fun collectImageNodes(root: ASTNode): List<ASTNode> {
    val list = mutableListOf<ASTNode>()
    fun visit(n: ASTNode) {
        if (n.type == MarkdownElementTypes.IMAGE) list += n
        n.children.forEach { visit(it) }
    }
    visit(root)
    return list
}

internal data class BlockImageRange(val url: String, val start: Int, val end: Int, val imageNode: ASTNode?)

@Composable
private fun BlockFallbackImage(url: String) {
    LocalImageTransformer.current.transform(url)?.let { imageData ->
        Image(
            painter = imageData.painter,
            contentDescription = imageData.contentDescription,
            modifier = imageData.modifier,
            alignment = imageData.alignment,
            contentScale = imageData.contentScale,
            alpha = imageData.alpha,
            colorFilter = imageData.colorFilter,
        )
    }
}

internal fun buildImageInlineContent(
    content: AnnotatedString,
    node: ASTNode,
    transformer: ImageTransformer,
    density: Density,
    containerSize: Size,
    inlineImageWidth: ImageWidth,
    imageSizeByLink: Map<String, Size>,
    defaultImageSize: Size = Size.Unspecified,
    lineHeightPx: Float = 0f,
    inlineImageAsBlock: Boolean = true,
    imageNodes: List<ASTNode> = emptyList(),
    onBlockImage: ((BlockImageRange) -> Unit)? = null,
    imageSizeChanged: ((link: String, Size) -> Unit)? = null,
): Map<String, InlineTextContent> {
    val annotations = content.getStringAnnotations(0, content.length)
        .filter { it.item.startsWith("${MARKDOWN_TAG_IMAGE_URL}_") }
        .sortedBy { it.start }

    fun shouldPromote(url: String): Boolean {
        val imageSize = imageSizeByLink[url] ?: defaultImageSize
        return inlineImageAsBlock &&
                lineHeightPx > 0f &&
                !imageSize.isUnspecified &&
                imageSize.height > lineHeightPx * MarkdownAnnotatorConfig.BLOCK_FALLBACK_LINE_MULTIPLIER
    }

    annotations.forEachIndexed { index, annotation ->
        val url = annotation.item.removePrefix("${MARKDOWN_TAG_IMAGE_URL}_")
        if (shouldPromote(url)) {
            onBlockImage?.invoke(
                BlockImageRange(
                    url = url,
                    start = annotation.start,
                    end = annotation.end,
                    imageNode = imageNodes.getOrNull(index),
                )
            )
        }
    }

    val byTag = annotations.groupBy { it.item }
    return byTag.mapNotNull { (tag, _) ->
        val url = tag.removePrefix("${MARKDOWN_TAG_IMAGE_URL}_")
        if (shouldPromote(url)) return@mapNotNull null

        val imageSize = imageSizeByLink[url] ?: defaultImageSize
        val config = transformer.placeholderConfig(
            url,
            density,
            containerSize,
            inlineImageWidth,
            imageSize,
            imageSizeChanged,
        )

        // Config size is in DP, convert to SP for Placeholder TextUnit
        tag to InlineTextContent(
            Placeholder(
                width = with(density) { config.size.width.dp.toSp() },
                height = with(density) { config.size.height.dp.toSp() },
                placeholderVerticalAlign = config.verticalAlign
            )
        ) { link ->
            // Render the image and observe its intrinsic size
            MarkdownInlineImageWithSize(
                link = link,
                node = node,
                transformer = transformer,
                density = density,
                onSizeDetected = { detectedSize ->
                    // Update size cache when image loads
                    imageSizeChanged?.invoke(url, detectedSize)
                }
            )
        }
    }.toMap()
}

/**
 * Scan the [content] [AnnotatedString] for `MARKDOWN_MATH_<latex>` inline
 * placeholders inserted by [com.ismartcoding.plain.lib.markdown.annotator.buildMarkdownAnnotatedString]
 * (when it walks a paragraph that contains an inline `$…$` or block
 * `$$…$$` math node) and produce an [InlineTextContent] map for each. The
 * placeholder body is the full LaTeX source including delimiters, which is
 * what the `huarangmeng/latex` renderer expects.
 *
 * Placeholder size is a heuristic: one line of `sp` text. The actual
 * rendered math can grow wider / taller than the placeholder; we just need
 * a non-zero slot so Compose lays out surrounding text in the right place.
 */
internal fun buildMathInlineContent(
    content: AnnotatedString,
    fontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
): Map<String, InlineTextContent> {
    val annotations = content.getStringAnnotations(0, content.length)
        .filter { it.item.startsWith("${MARKDOWN_TAG_MATH}_") }
    if (annotations.isEmpty()) return emptyMap()
    return annotations.associate { annotation ->
        val latex = annotation.item.removePrefix("${MARKDOWN_TAG_MATH}_")
        // `$$…$$` block math tends to be wide (a multi-term equation can
        // span 15-20em), but `InlineTextContent` caps the rendered
        // composable at the placeholder's width. We size the placeholder
        // to roughly 20em so even a long block formula has room to draw
        // before being clipped by the text layout. Inline `$…$` math is
        // kept narrow so it sits on the text baseline cleanly.
        val isBlock = latex.startsWith("$$")
        val placeholderWidth = if (isBlock) fontSize.value * 20f else fontSize.value * 1.2f
        val placeholderHeight = if (isBlock) fontSize.value * 3f else fontSize.value * 1.4f
        annotation.item to InlineTextContent(
            Placeholder(
                width = placeholderWidth.sp,
                height = placeholderHeight.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            )
        ) { src ->
            MarkdownMath(latex = src)
        }
    }
}

@Composable
private fun MarkdownInlineImageWithSize(
    link: String,
    node: ASTNode,
    transformer: ImageTransformer,
    density: Density,
    onSizeDetected: (Size) -> Unit,
) {
    val imageData = transformer.transform(link)

    // Detect intrinsic size when painter becomes available
    val intrinsicSize = imageData?.let { transformer.intrinsicSize(it.painter) } ?: Size.Unspecified
    if (intrinsicSize != Size.Unspecified) {
        SideEffect { onSizeDetected(intrinsicSize) }
    }

    // Delegate to the customizable component
    LocalMarkdownComponents.current.inlineImage(
        MarkdownComponentModel(link, node, LocalMarkdownTypography.current)
    )
}
