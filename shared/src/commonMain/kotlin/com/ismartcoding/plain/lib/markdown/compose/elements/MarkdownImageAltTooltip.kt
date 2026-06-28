package com.ismartcoding.plain.lib.markdown.compose.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownAnnotator
import kotlinx.coroutines.delay

/**
 * Wraps [content] with a small popup that surfaces [alt] on hover, gated by
 * [com.ismartcoding.plain.lib.markdown.model.MarkdownAnnotatorConfig.showImageAltTooltip].
 * The hover dwell time is read from
 * [com.ismartcoding.plain.lib.markdown.model.MarkdownAnnotatorConfig.imageAltTooltipHoverDelayMs].
 */
@Composable
internal fun ImageAltTooltip(alt: String?, content: @Composable () -> Unit) {
    val config = LocalMarkdownAnnotator.current.config
    if (!config.showImageAltTooltip || alt.isNullOrBlank()) {
        content()
        return
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val transitionState = remember { MutableTransitionState(false) }

    LaunchedEffect(isHovered, config.imageAltTooltipHoverDelayMs) {
        if (isHovered) {
            delay(config.imageAltTooltipHoverDelayMs)
            transitionState.targetState = true
        } else {
            transitionState.targetState = false
        }
    }

    // Keep the popup mounted while either the target is visible or the
    // exit animation is still running, so fadeOut can play before unmount.
    val mounted = transitionState.currentState || transitionState.targetState

    Box(modifier = Modifier.hoverable(interactionSource)) {
        content()
        if (mounted) {
            Popup(popupPositionProvider = AnchorBelowPositionProvider) {
                AnimatedVisibility(
                    visibleState = transitionState,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        BasicText(
                            text = alt,
                            style = TextStyle(color = Color.White, fontSize = 12.sp),
                        )
                    }
                }
            }
        }
    }
}

private val AnchorBelowPositionProvider = object : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val y = (anchorBounds.bottom + 4).coerceAtMost(
            (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        )
        return IntOffset(x, y)
    }
}
