package com.ismartcoding.plain.lib.markdown.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Helper to use when debugging recompositions.
 */
@Composable
@Suppress("NOTHING_TO_INLINE")
inline fun LogCompositions(
    crossinline message: () -> String,
) {
    if (MarkdownLogger.enabled) {
        var ref by remember { mutableStateOf(0) }
        SideEffect { ref++ }
        MarkdownLogger.d("Compositions") {
            "${message()} $ref"
        }
    }
}