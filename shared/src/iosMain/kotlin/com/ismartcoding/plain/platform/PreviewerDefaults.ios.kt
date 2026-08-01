package com.ismartcoding.plain.platform

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem

val DEFAULT_PREVIEWER_ENTER_TRANSITION: EnterTransition = scaleIn(tween(180)) + fadeIn(tween(240))
val DEFAULT_PREVIEWER_EXIT_TRANSITION: ExitTransition = scaleOut(tween(320)) + fadeOut(tween(240))
val DEFAULT_PLACEHOLDER_ENTER_TRANSITION: EnterTransition = fadeIn(tween(200))
val DEFAULT_PLACEHOLDER_EXIT_TRANSITION: ExitTransition = fadeOut(tween(200))

val DEFAULT_PREVIEWER_PLACEHOLDER_CONTENT: @Composable () -> Unit = {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White.copy(0.2F))
    }
}

class PreviewerPlaceholder(
    var enterTransition: EnterTransition = DEFAULT_PLACEHOLDER_ENTER_TRANSITION,
    var exitTransition: ExitTransition = DEFAULT_PLACEHOLDER_EXIT_TRANSITION,
    var content: @Composable () -> Unit = DEFAULT_PREVIEWER_PLACEHOLDER_CONTENT,
)

/**
 * iOS variant of `getModel`. Returns the [PreviewItem] itself — Coil3 handles
 * image loading on iOS via the cross-platform `AsyncImage` composable, so
 * there is no need for the Android `ImageDecoder` huge-image path or a
 * separate `Painter` factory.
 */
@Composable
fun getModel(item: PreviewItem): Any = item
