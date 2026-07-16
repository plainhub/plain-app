package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.models.DlnaReceiverViewModel

/**
 * DLNA receiver audio player surface.
 * Android: ExoPlayer-backed audio player with album art display.
 * iOS: stub (DLNA receiver not supported on iOS).
 */
@Composable
expect fun DlnaReceiverAudioPlayerContent(vm: DlnaReceiverViewModel, onExit: () -> Unit)

/**
 * DLNA receiver video player surface.
 * Android: ExoPlayer + PlayerView with custom controls overlay.
 * iOS: stub (DLNA receiver not supported on iOS).
 */
@Composable
expect fun DlnaReceiverVideoPlayerContent(vm: DlnaReceiverViewModel, onExit: () -> Unit)

/**
 * DLNA receiver image viewer surface.
 * Android: coil3 AsyncImage with fullscreen controls.
 * iOS: stub (DLNA receiver not supported on iOS).
 */
@Composable
expect fun DlnaReceiverImageViewerContent(onExit: () -> Unit)
