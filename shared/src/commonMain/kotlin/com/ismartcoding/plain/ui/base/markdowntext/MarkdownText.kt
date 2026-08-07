package com.ismartcoding.plain.ui.base.markdowntext

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Size as CoilSize
import com.ismartcoding.plain.enums.MarkdownTheme
import com.ismartcoding.plain.extensions.getFinalPath
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.lib.markdown.annotator.annotatorSettings
import com.ismartcoding.plain.lib.markdown.compose.Markdown
import com.ismartcoding.plain.lib.markdown.compose.components.MarkdownComponentModel
import com.ismartcoding.plain.lib.markdown.compose.components.markdownComponents
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownText as CoreMarkdownTextElement
import com.ismartcoding.plain.lib.markdown.compose.extendedspans.ExtendedSpans
import com.ismartcoding.plain.lib.markdown.compose.extendedspans.RoundedCornerSpanPainter
import com.ismartcoding.plain.lib.markdown.model.ImageData
import com.ismartcoding.plain.lib.markdown.model.ImageTransformer
import com.ismartcoding.plain.lib.markdown.model.markdownExtendedSpans
import com.ismartcoding.plain.lib.markdown.compose.elements.RenderMathNode
import com.ismartcoding.plain.lib.markdown.utils.getUnescapedTextInNode
import kotlinx.coroutines.launch
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes

private val IMAGE_MARKDOWN_REGEX = Regex("""!\[.*?]\(([^)\s]+)\)""")
// Match the `src="..."` (or `src='...'`, or unquoted) attribute of an HTML
// `<img>` tag. We accept the first `src` we see on each `<img>` and ignore the
// rest. This mirrors the same lenient parser the markdown library uses in
// [com.ismartcoding.plain.lib.markdown.utils.HTML_IMG_TAG_REGEX].
private val HTML_IMG_SRC_REGEX = Regex(
    """<img\b[^>]*?\bsrc\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))[^>]*>""",
    RegexOption.IGNORE_CASE,
)

private fun extractImageLinksFromMarkdown(markdown: String): List<String> {
    // Pull every image source URL out of the markdown document, regardless
    // of which syntax produced it. The two forms — `![alt](src)` and HTML
    // `<img src="...">` — have to be merged into a single ordered gallery
    // so the previewer shows images in the order they actually appear in the
    // note. Each match carries its source-range start offset, which
    // is a stable tiebreaker that preserves document order across the
    // two regexes.
    val markdownLinks = IMAGE_MARKDOWN_REGEX.findAll(markdown)
        .map { it.range.first to it.groupValues[1] }
    val htmlImgLinks = HTML_IMG_SRC_REGEX.findAll(markdown)
        .map { match ->
            // groups: 0=full, 1="...", 2='...', 3=unquoted — pick the first non-empty.
            match.range.first to match.groupValues.drop(1).first { it.isNotEmpty() }
        }
    return (markdownLinks + htmlImgLinks)
        .sortedBy { it.first }
        .map { it.second }
        .toList()
}

/**
 * Image transformer that resolves project-internal `app://` and `fid:` URIs to absolute file
 * paths before delegating to Coil3. This preserves the behaviour of the previous Markwon-based
 * `AppImageSchemeHandler` for in-app asset references.
 *
 * The transformer also wires the tap handler that opens
 * [MediaPreviewerState] when the user clicks an image. The handler is
 * installed via [ImageData.onClick] (added to the `ImageData` API in
 * support of clickable previews — the upstream `MarkdownImage` composable
 * wraps the painter in a `Modifier.clickable` when this is non-null).
 */
private class AppImageTransformer(
    private val onImageClick: (String) -> Unit,
) : ImageTransformer {
    @Composable
    override fun transform(link: String): ImageData? {
        val context = LocalPlatformContext.current
        val resolved = if (link.startsWith("app://", ignoreCase = true) ||
            link.startsWith("fid:", ignoreCase = true)
        ) {
            link.getFinalPath()
        } else {
            link
        }
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(resolved)
                .size(CoilSize.ORIGINAL)
                .listener(
                    onError = { _, result ->
                        LogCat.e("MavisMd: Coil error data='$resolved' throwable=${result.throwable.message}")
                    },
                )
                .build()
        )
        return ImageData(
            painter = painter,
            contentDescription = link,
            onClick = { onImageClick(link) },
        )
    }
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    truncateOnTextOverflow: Boolean = false,
    isTextSelectable: Boolean = true,
    style: TextStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    previewerState: MediaPreviewerState,
) {
    val scope = rememberCoroutineScope()
    val defaultColor = MaterialTheme.colorScheme.onSurface

    val textStyle = remember(style, defaultColor) {
        TextStyle(
            color = style.color.takeOrElse { defaultColor },
            fontSize = style.fontSize,
            lineHeight = style.lineHeight,
            textAlign = style.textAlign,
            fontStyle = style.fontStyle ?: FontStyle.Normal,
            fontWeight = style.fontWeight ?: FontWeight.Normal,
            textDecoration = style.textDecoration ?: TextDecoration.None,
        )
    }

    val colors = MarkdownTheme.colors()
    val typography = MarkdownTheme.typography(textStyle)
    val padding = MarkdownTheme.padding()
    val dimens = MarkdownTheme.dimens()

    val inlineCodeFontSize = typography.inlineCode.fontSize
    val inlineCodeLineHeight = typography.inlineCode.lineHeight
    val inlineCodeMargin = run {
        val fontSizePx = if (inlineCodeFontSize.isSpecified) inlineCodeFontSize.value else 14f
        val lineHeightPx = if (inlineCodeLineHeight.isSpecified) inlineCodeLineHeight.value else fontSizePx
        ((lineHeightPx - fontSizePx) / 2f).sp
    }

    val extendedSpans = markdownExtendedSpans {
        ExtendedSpans(
            RoundedCornerSpanPainter(
                cornerRadius = 4.sp,
                padding = RoundedCornerSpanPainter.TextPaddingValues(horizontal = 5.sp, vertical = 2.sp),
                topMargin = 0.sp,
                bottomMargin = 0.sp,
            ),
        )
    }

    val components = remember(text) {
        markdownComponents(
            text = { model: MarkdownComponentModel ->
                val settings = annotatorSettings(
                    linkInteractionListener = LinkInteractionListener { link ->
                        val url = (link as? LinkAnnotation.Url)?.url ?: return@LinkInteractionListener
                        WebHelper.open(url)
                    },
                )
                CoreMarkdownTextElement(
                    content = model.node.getUnescapedTextInNode(model.content),
                    node = model.node,
                    style = model.typography.text,
                    annotatorSettings = settings,
                )
            },
            checkbox = { model ->
                val checked = model.node.getTextInNode(model.content).contains("[x]")
                val primary = MaterialTheme.colorScheme.primary
                val outline = MaterialTheme.colorScheme.outlineVariant
                val onPrimary = MaterialTheme.colorScheme.onPrimary
                val shape = RoundedCornerShape(6.dp)
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(18.dp)
                        .clip(shape)
                        .then(
                            if (checked) Modifier.background(primary)
                            else Modifier.border(1.5.dp, outline, shape)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (checked) {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            val path = Path().apply {
                                moveTo(size.width * 0.18f, size.height * 0.52f)
                                lineTo(size.width * 0.42f, size.height * 0.75f)
                                lineTo(size.width * 0.82f, size.height * 0.28f)
                            }
                            drawPath(
                                path = path,
                                color = onPrimary,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                            )
                        }
                    }
                }
            },
            custom = { type, model ->
                if (type == GFMElementTypes.BLOCK_MATH || type == GFMElementTypes.INLINE_MATH) {
                    RenderMathNode(model.content, model.node)
                }
            },
        )
    }

    Markdown(
        content = text,
        modifier = modifier,
        colors = colors,
        typography = typography,
        padding = padding,
        dimens = dimens,
        extendedSpans = extendedSpans,
        imageTransformer = remember(text) {
            AppImageTransformer(onImageClick = { link ->
                val imageLinks = extractImageLinksFromMarkdown(text)
                val items = imageLinks.map { src ->
                    PreviewItem(src, src.getFinalPath())
                }
                MediaPreviewData.items = items
                val index = items.indexOfFirst { it.id == link || it.path == link }
                scope.launch {
                    previewerState.open(index = index.coerceAtLeast(0))
                }
            })
        },
        components = components,
    )

    // truncateOnTextOverflow and isTextSelectable flags are accepted for API compatibility with
    // the previous Markwon-based implementation. The core library does not expose a direct
    // maxLines / selection toggle on the Markdown composable; selection is already provided by
    // MarkdownBasicText, and truncation is the caller's responsibility if needed.
    @Suppress("UNUSED_VARIABLE") val _t = truncateOnTextOverflow
    @Suppress("UNUSED_VARIABLE") val _s = isTextSelectable
}
