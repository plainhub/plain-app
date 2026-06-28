package com.ismartcoding.plain.ui.base.markdowntext

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Size as CoilSize
import com.ismartcoding.plain.lib.extensions.getFinalPath
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.lib.markdown.annotator.annotatorSettings
import com.ismartcoding.plain.lib.markdown.compose.Markdown
import com.ismartcoding.plain.lib.markdown.compose.components.MarkdownComponentModel
import com.ismartcoding.plain.lib.markdown.compose.components.markdownComponents
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownText as CoreMarkdownTextElement
import com.ismartcoding.plain.lib.markdown.model.DefaultMarkdownColors
import com.ismartcoding.plain.lib.markdown.model.DefaultMarkdownTypography
import com.ismartcoding.plain.lib.markdown.model.ImageData
import com.ismartcoding.plain.lib.markdown.model.ImageTransformer
import com.ismartcoding.plain.lib.markdown.model.markdownDimens
import com.ismartcoding.plain.lib.markdown.model.markdownPadding
import com.ismartcoding.plain.lib.markdown.compose.elements.RenderMathNode
import com.ismartcoding.plain.lib.markdown.utils.getUnescapedTextInNode
import kotlinx.coroutines.launch
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
    // so the previewer shows images in the order they actually appear in
    // the note. Each match carries its source-range start offset, which
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
            // androidMain-only extension; this file lives in app/ (Android-only).
            link.getFinalPath(context)
        } else {
            link
        }
        // Log only failures — the happy path produces enough noise on its own
        // when combined with Coil's internal logging. `onError` is the
        // single most useful signal for diagnosing https / SAN issues like
        // `SSLPeerUnverifiedException`.
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(resolved)
                .size(CoilSize.ORIGINAL)
                .listener(
                    onError = { _, result ->
                        android.util.Log.e(
                            "MavisMd",
                            "Coil error data='$resolved' throwable=${result.throwable.message}",
                            result.throwable,
                        )
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val defaultColor = MaterialTheme.colorScheme.onSurface
    val linkTextColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val textStyle = remember(style, defaultColor) {
        TextStyle(
            color = style.color.takeOrElse { defaultColor },
            fontSize = style.fontSize,
            lineHeight = style.lineHeight,
            textAlign = style.textAlign ?: TextAlign.Unspecified,
            fontStyle = style.fontStyle ?: FontStyle.Normal,
            fontWeight = style.fontWeight ?: FontWeight.Normal,
            textDecoration = style.textDecoration ?: TextDecoration.None,
        )
    }

    val monoStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        color = onSurfaceVariant,
    )

    val colors = DefaultMarkdownColors(
        text = defaultColor,
        codeBackground = surfaceVariant,
        inlineCodeBackground = surfaceVariant,
        dividerColor = outlineVariant,
        tableBackground = surfaceVariant,
    )

    val typography = DefaultMarkdownTypography(
        h1 = textStyle.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
        h2 = textStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
        h3 = textStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        h4 = textStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        h5 = textStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        h6 = textStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        text = textStyle,
        code = monoStyle,
        inlineCode = monoStyle.copy(background = surfaceVariant),
        quote = textStyle.copy(color = onSurfaceVariant),
        paragraph = textStyle,
        ordered = textStyle,
        bullet = textStyle,
        list = textStyle,
        textLink = TextLinkStyles(style = SpanStyle(color = linkTextColor, textDecoration = TextDecoration.Underline)),
        table = textStyle,
    )

    // Custom link click handling: image links open the media previewer, others open in browser.
    // Default components.text does NOT build AnnotatedString with LinkAnnotations, so we override
    // components.text to use the String-overload that accepts AnnotatorSettings.
    val components = remember(text, linkTextColor, defaultColor) {
        markdownComponents(
            text = { model: MarkdownComponentModel ->
                val settings = annotatorSettings(
                    linkTextSpanStyle = TextLinkStyles(
                        style = SpanStyle(color = linkTextColor, textDecoration = TextDecoration.Underline)
                    ),
                    codeSpanStyle = SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = onSurfaceVariant,
                        background = surfaceVariant,
                    ),
                    linkInteractionListener = LinkInteractionListener { link ->
                        // Image taps are routed through `ImageData.onClick` (see
                        // `AppImageTransformer`), so a `LinkAnnotation` reaching
                        // this listener is always a real markdown link. The
                        // previous implementation cross-checked against
                        // `extractImageLinksFromMarkdown` here, which became
                        // dead code once `MarkdownImage` grew a dedicated
                        // `clickable` modifier.
                        val url = (link as? LinkAnnotation.Url)?.url ?: return@LinkInteractionListener
                        WebHelper.open(context, url)
                    },
                )
                CoreMarkdownTextElement(
                    content = model.node.getUnescapedTextInNode(model.content),
                    node = model.node,
                    style = model.typography.text,
                    annotatorSettings = settings,
                )
            },
            // Intercept GFM math nodes ($...$ and $$...$$) and render them through
            // MTMathView instead of the default text fallback (which would otherwise
            // surface the raw LaTeX source). Returning Unit signals "handled" to the
            // MarkdownElementInternal dispatcher.
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
        padding = markdownPadding(),
        dimens = markdownDimens(),
        imageTransformer = remember(text) {
            AppImageTransformer(onImageClick = { link ->
                val imageLinks = extractImageLinksFromMarkdown(text)
                val items = imageLinks.map { src ->
                    PreviewItem(src, src.getFinalPath(context))
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
