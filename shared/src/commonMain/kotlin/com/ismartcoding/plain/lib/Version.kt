package com.ismartcoding.plain.lib

/**
 * Platform SDK version checks, KMP-ified.
 *
 * Android: maps to `Build.VERSION.SDK_INT` comparisons against `Build.VERSION_CODES.*`.
 * iOS / Native: always returns `true` (iOS has no SDK level gating for our features).
 */
expect fun isP(): Boolean
expect fun isQPlus(): Boolean
expect fun isRPlus(): Boolean
expect fun isSPlus(): Boolean
expect fun isSV2Plus(): Boolean
expect fun isTPlus(): Boolean
expect fun isUPlus(): Boolean
expect fun isVPlus(): Boolean
expect fun isBPlus(): Boolean
