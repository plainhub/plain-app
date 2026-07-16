package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.models.DlnaReceiverViewModel

@Composable
actual fun DlnaReceiverAudioPlayerContent(vm: DlnaReceiverViewModel, onExit: () -> Unit) {}

@Composable
actual fun DlnaReceiverVideoPlayerContent(vm: DlnaReceiverViewModel, onExit: () -> Unit) {}

@Composable
actual fun DlnaReceiverImageViewerContent(onExit: () -> Unit) {}
