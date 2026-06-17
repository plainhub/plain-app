package com.ismartcoding.plain.helpers

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Platform-agnostic IO dispatcher.
 *
 * On Android, this maps to `Dispatchers.IO` (a dedicated thread pool sized for blocking IO).
 * On iOS / Native, this falls back to `Dispatchers.Default` (the closest KMP-compatible
 * equivalent, since `Dispatchers.IO` is not available on Native targets).
 *
 * Use this instead of referencing `Dispatchers.IO` directly in commonMain, which is JVM-only.
 */
expect val IODispatcher: CoroutineDispatcher