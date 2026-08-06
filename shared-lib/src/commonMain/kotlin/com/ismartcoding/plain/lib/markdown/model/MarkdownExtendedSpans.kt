package com.ismartcoding.plain.lib.markdown.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.ismartcoding.plain.lib.markdown.compose.extendedspans.ExtendedSpans

@Stable
@Immutable
interface MarkdownExtendedSpans {
    val extendedSpans: (@Composable () -> ExtendedSpans)?
}

@Stable
@Immutable
class DefaultMarkdownExtendedSpans(
    override val extendedSpans: (@Composable () -> ExtendedSpans)?,
) : MarkdownExtendedSpans {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultMarkdownExtendedSpans

        return extendedSpans == other.extendedSpans
    }

    override fun hashCode(): Int {
        return extendedSpans?.hashCode() ?: 0
    }
}

@Composable
fun markdownExtendedSpans(
    extendedSpans: (@Composable () -> ExtendedSpans)? = null,
): MarkdownExtendedSpans = DefaultMarkdownExtendedSpans(extendedSpans)
