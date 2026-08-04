package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem

/**
 * iOS variant of `getModel`. Returns the [PreviewItem] itself — Coil3 handles
 * image loading on iOS via the cross-platform `AsyncImage` composable, so
 * there is no need for the Android `ImageDecoder` huge-image path or a
 * separate `Painter` factory.
 */
@Composable
actual fun getModel(item: PreviewItem): Any = item
