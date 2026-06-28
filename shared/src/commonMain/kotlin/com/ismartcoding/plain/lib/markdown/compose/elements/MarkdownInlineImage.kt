package com.ismartcoding.plain.lib.markdown.compose.elements

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.lib.markdown.compose.LocalImageTransformer
import org.intellij.markdown.ast.ASTNode

/**
 * Render the inline image placeholder supplied by [MarkdownText].
 *
 * Note: this entry point is only reached when the image is rendered
 * through the [androidx.compose.foundation.text.InlineTextContent] path
 * (e.g. inside table cells, where the renderer does not promote the
 * image to a block). For top-level paragraph images, the
 * `PARAGRAPH` dispatcher in [com.ismartcoding.plain.lib.markdown.compose.MarkdownElementInternal]
 * detects inline images and routes them through
 * [com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownImage]
 * directly, because [androidx.compose.foundation.text.BasicText] consumes
 * every pointer event for its own text-selection / LinkAnnotation logic
 * and the inline placeholder's child composable cannot receive taps —
 * which is exactly what we need for the tap-to-open-preview behaviour.
 *
 * The `ImageData.onClick` field is therefore unused on this code path;
 * the comment on [com.ismartcoding.plain.lib.markdown.model.ImageData.onClick]
 * explains the contract.
 */
@Composable
fun MarkdownInlineImage(link: String, node: ASTNode, alt: String? = null) {
    val transformer = LocalImageTransformer.current
    transformer.transform(link)?.let { imageData ->
        Image(
            painter = imageData.painter,
            contentDescription = alt ?: imageData.contentDescription,
            modifier = Modifier.fillMaxSize().then(imageData.modifier),
            alignment = imageData.alignment,
            contentScale = imageData.contentScale,
            alpha = imageData.alpha,
            colorFilter = imageData.colorFilter
        )
    }
}
