package com.ismartcoding.plain.lib.markdown.model

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
@Immutable
interface MarkdownInlineContent {
    /** Represents the map used to store tags and corresponding inline content */
    val inlineContent: Map<String, InlineTextContent>
}

@Stable
@Immutable
class DefaultMarkdownInlineContent(
    override val inlineContent: Map<String, InlineTextContent>,
) : MarkdownInlineContent {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultMarkdownInlineContent

        return inlineContent == other.inlineContent
    }

    override fun hashCode(): Int {
        return inlineContent.hashCode()
    }
}

@Composable
fun markdownInlineContent(
    content: Map<String, InlineTextContent> = mapOf(),
): MarkdownInlineContent = DefaultMarkdownInlineContent(content)
