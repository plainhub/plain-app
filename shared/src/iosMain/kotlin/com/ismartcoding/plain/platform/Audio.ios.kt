package com.ismartcoding.plain.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val stubIsPlayingFlow = MutableStateFlow(false)

actual fun audioIsPlayingFlow(): StateFlow<Boolean> = stubIsPlayingFlow

actual fun audioPlayerProgress(): Long = 0L

actual fun audioSeekTo(progress: Long) {}

actual fun audioPause() {}

actual fun audioPlay() {}

actual fun playAudioFromPath(path: String) {}

actual fun playAudioWithNotificationCheck(path: String) {}
