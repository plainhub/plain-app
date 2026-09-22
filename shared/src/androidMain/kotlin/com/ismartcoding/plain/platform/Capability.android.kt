package com.ismartcoding.plain.platform

import android.os.Build

actual fun getDeviceCapabilities(): List<Capability> =
    Capability.entries
        // MEDIA_SCAN is the NAS media-index scanner; phones don't have it.
        .filter { it != Capability.MEDIA_SCAN }
        .filter { feature ->
            val minSdk = when (feature) {
                // MediaStore trashed-state
                Capability.MEDIA_TRASH -> Build.VERSION_CODES.R
                // AudioPlaybackCapture
                Capability.MIRROR_AUDIO -> Build.VERSION_CODES.Q
                else -> 0
            }
            Build.VERSION.SDK_INT >= minSdk
        }
