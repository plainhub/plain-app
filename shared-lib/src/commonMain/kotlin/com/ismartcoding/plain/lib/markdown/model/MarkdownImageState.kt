package com.ismartcoding.plain.lib.markdown.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

interface MarkdownImageState {
    val density: Density
    val containerSize: Size
    val intrinsicImageSize: Size
    
    fun updateContainerSize(size: Size)

    fun updateImageSize(size: Size)
}

internal class MarkdownImageStateImpl(override val density: Density) : MarkdownImageState {

    override var containerSize by mutableStateOf(Size.Unspecified)

    override var intrinsicImageSize by mutableStateOf(Size.Unspecified)

    override fun updateContainerSize(size: Size) {
        containerSize = size
    }

    override fun updateImageSize(size: Size) {
        intrinsicImageSize = size
    }
}

/**
 * Creates and remembers a [MarkdownImageState] instance.
 *
 * This composable function creates a new instance of [MarkdownImageState] using the current
 * density from [LocalDensity] and remembers it across recompositions as long as the
 * density doesn't change.
 *
 * It's used internally by the markdown renderer to manage image state for inline images.
 *
 * @return A remembered instance of [MarkdownImageState].
 */
@Composable
internal fun rememberMarkdownImageState(): MarkdownImageState {
    val density = LocalDensity.current
    return remember(density) { MarkdownImageStateImpl(density) }
}
