package com.ismartcoding.plain.lib.markdown.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import org.intellij.markdown.ast.ASTNode

@Stable
@Immutable
interface MarkdownAnnotator {

    /**
     * Use the [AnnotatedString.Builder] to build the string to display.
     * Return `true` to consume the child, false to allow default handling.
     *
     * @param content contains the whole content, and requires the `child` [ASTNode] to extract relevant text.
     */
    val annotate: (AnnotatedString.Builder.(content: String, child: ASTNode) -> Boolean)?

    /** Defines static configuration for the [androidx.compose.ui.text.AnnotatedString] annotator */
    val config: MarkdownAnnotatorConfig
}

@Stable
@Immutable
class DefaultMarkdownAnnotator(
    override val annotate: (AnnotatedString.Builder.(content: String, child: ASTNode) -> Boolean)?,
    override val config: MarkdownAnnotatorConfig,
) : MarkdownAnnotator {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultMarkdownAnnotator

        if (annotate != other.annotate) return false
        if (config != other.config) return false

        return true
    }

    override fun hashCode(): Int {
        var result = annotate?.hashCode() ?: 0
        result = 31 * result + config.hashCode()
        return result
    }
}

fun markdownAnnotator(
    config: MarkdownAnnotatorConfig = markdownAnnotatorConfig(),
    annotate: (AnnotatedString.Builder.(content: String, child: ASTNode) -> Boolean)? = null,
): MarkdownAnnotator = DefaultMarkdownAnnotator(
    annotate = annotate,
    config = config
)
