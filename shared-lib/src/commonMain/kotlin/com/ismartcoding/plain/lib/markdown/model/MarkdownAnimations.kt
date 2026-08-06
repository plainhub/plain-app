package com.ismartcoding.plain.lib.markdown.model

import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownText

@Stable
@Immutable
interface MarkdownAnimations {
    /**
     * Modifier used to animate [MarkdownText] size changes.
     * This is mainly the case if inline images are loaded and their placeholder has a different size from the final image.
     */
    val animateTextSize: Modifier.() -> Modifier
}

@Stable
@Immutable
class DefaultMarkdownAnimation(
    override val animateTextSize: Modifier.() -> Modifier,
) : MarkdownAnimations {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultMarkdownAnimation

        return animateTextSize == other.animateTextSize
    }

    override fun hashCode(): Int {
        return animateTextSize.hashCode()
    }
}

@Composable
fun markdownAnimations(
    /**
     * Modifier used to animate [MarkdownText] size changes.
     * By default, this uses [animateContentSize].
     *
     * It's possible to modify the animation or alternatively return` {this} to not animate at all.
     */
    animateTextSize: Modifier.() -> Modifier = { animateContentSize() },
): MarkdownAnimations = DefaultMarkdownAnimation(
    animateTextSize
)
