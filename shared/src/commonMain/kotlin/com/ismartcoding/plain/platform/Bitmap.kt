package com.ismartcoding.plain.platform

/**
 * Combine the images located at [paths] into a single square thumbnail of [size] x [size].
 *
 * Returns a platform-specific image model (e.g. `android.graphics.Bitmap` on Android) that can be
 * consumed by coil3 `AsyncImage`, or `null` when no valid images are available or on platforms
 * without support (iOS stub returns `null`).
 */
expect suspend fun combineBitmapGrid(paths: List<String>, size: Int): Any?
