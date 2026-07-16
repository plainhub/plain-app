package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.ScreenMirrorControlInput
import com.ismartcoding.plain.enums.ScreenMirrorMode
import com.ismartcoding.plain.web.models.ScreenMirrorVideoCodec

/**
 * Whether the screen mirror service is currently running and producing frames.
 */
expect fun isScreenMirrorRunning(): Boolean

/**
 * Returns the video codec advertised by the active screen mirror pipeline, or
 * null if the pipeline is not running or the codec is not yet known.
 */
expect fun getScreenMirrorVideoCodec(): ScreenMirrorVideoCodec?

/**
 * Whether the accessibility service (used for screen mirror control) is enabled.
 */
expect fun isScreenMirrorControlEnabled(): Boolean

/**
 * Update the active screen mirror quality at runtime. No-op if the service is
 * not running or the platform has no screen mirror capability.
 */
expect fun onScreenMirrorQualityChanged(mode: ScreenMirrorMode)

/**
 * Stop the screen mirror service if it is running. Safe to call when not running.
 */
expect fun stopScreenMirror()

/**
 * Apply the cached quality preference to the running screen mirror service.
 * Used at start time to seed the service with the current user preference.
 */
expect fun applyScreenMirrorQualityPreference()

/**
 * Dispatch a remote-control event to the accessibility service. Returns true on
 * success, false if the service is not enabled or the platform doesn't support
 * screen mirror control.
 */
expect fun dispatchScreenMirrorControl(input: ScreenMirrorControlInput): Boolean

/**
 * Returns the screen size used by the accessibility service for coordinate
 * scaling. Returns (0, 0) on unsupported platforms.
 */
expect fun getAccessibilityScreenSize(): Pair<Int, Int>
