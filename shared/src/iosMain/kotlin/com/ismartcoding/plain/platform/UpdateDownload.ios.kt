package com.ismartcoding.plain.platform

import com.ismartcoding.plain.events.UpdateDownloadFailedEvent
import com.ismartcoding.plain.lib.channel.sendEvent

actual fun downloadUpdateAsync() {
    // iOS does not support APK-style self-update; emit a failure event so the
    // UI shows an error instead of hanging on "downloading…".
    sendEvent(UpdateDownloadFailedEvent())
}

actual fun cancelUpdateDownloadAsync() {}
