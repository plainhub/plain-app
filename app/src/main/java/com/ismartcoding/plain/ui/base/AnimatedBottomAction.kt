package com.ismartcoding.plain.ui.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable

@Composable
fun AnimatedBottomAction(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    ) {
        content()
    }
}
