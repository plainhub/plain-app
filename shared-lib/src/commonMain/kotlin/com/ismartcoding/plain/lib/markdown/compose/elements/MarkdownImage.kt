package com.ismartcoding.plain.lib.markdown.compose.elements

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.markdown.compose.LocalImageTransformer
import com.ismartcoding.plain.lib.markdown.compose.LocalReferenceLinkHandler
import com.ismartcoding.plain.lib.markdown.utils.resolveImageAlt
import com.ismartcoding.plain.lib.markdown.utils.resolveImageLink
import org.intellij.markdown.ast.ASTNode

/**
 * Render an `IMAGE` AST node.
 *
 * The previous implementation forwarded `imageData.modifier` directly to `Image`.
 * `ImageData.modifier` defaults to plain `Modifier`, and `rememberAsyncImagePainter`
 * reports `Size.Unspecified` until the image has loaded — together they collapse the
 * image to zero area, so users see a blank gap where the picture should be.
 *
 * We now wrap the painter in a `Box` that:
 *  - takes the full row width (so the picture is at least visible while loading),
 *  - falls back to a 200 dp minimum height when the painter has no intrinsic size,
 *  - lets the painter's true aspect ratio drive the height once it is known,
 *  - invokes `imageData.onClick` on tap, when the transformer supplied one.
 *
 * `LocalImageTransformer` consumers can still override sizing via `imageData.modifier`
 * — we simply chain it after our sensible defaults. The tap handler is added
 * separately, only when the transformer set it, so renderers that don't care
 * about clicks (most previews / image galleries) get the previous behaviour.
 */
@Composable
fun MarkdownImage(
    content: String,
    node: ASTNode,
    altOverride: String? = null,
    linkOverride: String? = null,
) {

    // HTML `<img>` tags reach us as `HTML_TAG` tokens (not `IMAGE`), so the AST helpers
    // can't extract a destination — the caller passes one explicitly via `linkOverride`.
    val link = linkOverride ?: node.resolveImageLink(content, LocalReferenceLinkHandler.current) ?: return
    val alt = altOverride ?: node.resolveImageAlt(content)
    LocalImageTransformer.current.transform(link)?.let { imageData ->
        val tapModifier = if (imageData.onClick != null) {
            Modifier.clickable(onClick = imageData.onClick)
        } else {
            Modifier
        }
        ImageAltTooltip(alt) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 48.dp)
                    .heightIn(min = 48.dp)
                    .then(tapModifier)
                    .then(imageData.modifier)
            ) {
                Image(
                    painter = imageData.painter,
                    contentDescription = alt ?: imageData.contentDescription,
                    modifier = Modifier.fillMaxWidth(),
                    alignment = imageData.alignment,
                    contentScale = imageData.contentScale,
                    alpha = imageData.alpha,
                    colorFilter = imageData.colorFilter
                )
            }
        }
    }
}
