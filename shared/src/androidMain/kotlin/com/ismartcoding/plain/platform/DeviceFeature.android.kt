package com.ismartcoding.plain.platform

import android.os.Build

actual fun getDeviceFeatures(): List<DeviceFeature> =
    DeviceFeature.entries.filter { feature ->
        val minSdk = when (feature) {
            // MediaStore trashed-state
            DeviceFeature.MEDIA_TRASH -> Build.VERSION_CODES.R
            // AudioPlaybackCapture
            DeviceFeature.MIRROR_AUDIO -> Build.VERSION_CODES.Q
        }
        Build.VERSION.SDK_INT >= minSdk
    }
