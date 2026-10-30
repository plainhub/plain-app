package com.ismartcoding.plain.ui.editor.markdown

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ismartcoding.plain.ui.markdown.model.State
import com.ismartcoding.plain.ui.markdown.model.rememberMarkdownState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Size as CoilSize
import com.ismartcoding.plain.ui.theme.MarkdownTheme
import com.ismartcoding.plain.ui.extensions.rememberImageResource
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.ui.media.viewer.PreviewItem
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.features.web.WebHelper
import com.ismartcoding.plain.ui.media.MediaPreviewData
import com.ismartcoding.plain.ui.markdown.annotator.annotatorSettings
import com.ismartcoding.plain.ui.markdown.compose.Markdown
import com.ismartcoding.plain.ui.markdown.compose.components.MarkdownComponentModel
import com.ismartcoding.plain.ui.markdown.compose.components.markdownComponents
import com.ismartcoding.plain.ui.markdown.compose.components.MarkdownComponents
import com.ismartcoding.plain.ui.markdown.compose.elements.MarkdownText as CoreMarkdownTextElement
import com.ismartcoding.plain.ui.markdown.compose.extendedspans.ExtendedSpans
import com.ismartcoding.plain.ui.markdown.compose.extendedspans.RoundedCornerSpanPainter
import com.ismartcoding.plain.ui.markdown.model.ImageData
import com.ismartcoding.plain.ui.markdown.model.ImageTransformer
import com.ismartcoding.plain.ui.markdown.model.MarkdownColors
import com.ismartcoding.plain.ui.markdown.model.MarkdownDimens
import com.ismartcoding.plain.ui.markdown.model.MarkdownExtendedSpans
import com.ismartcoding.plain.ui.markdown.model.MarkdownPadding
import com.ismartcoding.plain.ui.markdown.model.MarkdownTypography
import com.ismartcoding.plain.ui.markdown.model.markdownExtendedSpans
import com.ismartcoding.plain.ui.markdown.compose.elements.RenderMathNode
import com.ismartcoding.plain.ui.markdown.utils.getUnescapedTextInNode
import kotlinx.coroutines.launch
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import com.ismartcoding.plain.features.files.FilePaths
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.navigation.LocalContentSource

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
internal class AppImageTransformer(
    private val onImageClick: ((String) -> Unit)?,
) : ImageTransformer {
    @Composable
    override fun transform(link: String): ImageData? {
        val context = LocalPlatformContext.current
        val resolved = rememberImageResource(link)
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
            onClick = onImageClick?.let { cb -> { cb(link) } },
        )
    }
}

/**
 * Immutable bundle of the theme-derived Markdown rendering settings so the
 * inline [MarkdownText] and lazy per-block renderers (feed reader) stay
 * visually identical while sharing one wiring.
 */
@Stable
class MarkdownTextConfig internal constructor(
    val colors: MarkdownColors,
    val typography: MarkdownTypography,
    val padding: MarkdownPadding,
    val dimens: MarkdownDimens,
    val extendedSpans: MarkdownExtendedSpans,
    val components: MarkdownComponents,
)

@Composable
fun rememberMarkdownTextConfig(
    style: TextStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
): MarkdownTextConfig {
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
    return remember(colors, typography, padding, dimens, extendedSpans) {
        MarkdownTextConfig(
            colors = colors,
            typography = typography,
            padding = padding,
            dimens = dimens,
            extendedSpans = extendedSpans,
            components = markdownComponents(
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
            ),
        )
    }
}

/**
 * Image transformer with the previewer click handler wired. No previewer state
 * (block editor) → images get no click handler and taps fall through to the
 * block's own edit affordance.
 */
@Composable
internal fun rememberMarkdownImageTransformer(
    parsed: State,
    previewerState: MediaPreviewerState?,
): ImageTransformer {
    val scope = rememberCoroutineScope()
    val source = LocalContentSource.current
    val imageLinks = remember(parsed) {
        (parsed as? State.Success)?.let(::extractImageLinksFromMarkdown).orEmpty()
    }
    return remember(imageLinks, previewerState, source) {
        AppImageTransformer(onImageClick = previewerState?.let { state -> { link ->
            scope.launch {
                val paths = if (source is ContentSource.Local) FilePaths.resolveAll(imageLinks) else imageLinks
                val items = imageLinks.mapIndexed { index, src ->
                    PreviewItem(src, paths[index], source = source)
                }
                MediaPreviewData.items = items
                val index = items.indexOfFirst { it.id == link || it.path == link }
                state.open(index = index.coerceAtLeast(0))
            }
        } })
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
    previewerState: MediaPreviewerState? = null,
) {
    val config = rememberMarkdownTextConfig(style)
    val markdownState = rememberMarkdownState(text)
    val parsed by markdownState.state.collectAsState()

    Markdown(
        state = parsed,
        modifier = modifier,
        colors = config.colors,
        typography = config.typography,
        padding = config.padding,
        dimens = config.dimens,
        extendedSpans = config.extendedSpans,
        imageTransformer = rememberMarkdownImageTransformer(parsed, previewerState),
        components = config.components,
    )

    // truncateOnTextOverflow and isTextSelectable flags are accepted for API compatibility with
    // the previous Markwon-based implementation. The core library does not expose a direct
    // maxLines / selection toggle on the Markdown composable; selection is already provided by
    // MarkdownBasicText, and truncation is the caller's responsibility if needed.
    @Suppress("UNUSED_VARIABLE") val _t = truncateOnTextOverflow
    @Suppress("UNUSED_VARIABLE") val _s = isTextSelectable
}
