package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.logcat.LogCat
import kotlin.math.abs

actual class SoundMeterDataSource {
    private val soundMeter = IosPlatformRegistry.soundMeter()

    actual fun start(): Boolean {
        val meter = soundMeter
        if (meter == null) {
            LogCat.e("SoundMeterDataSource: sound meter not registered")
            return false
        }
        if (!meter.start()) {
            LogCat.e("SoundMeterDataSource: failed to start sound meter")
            return false
        }
        return true
    }

    actual fun stop() {
        soundMeter?.stop()
    }

    actual fun getDecibel(): Float {
        val peakDbFS = soundMeter?.peakPower() ?: return Float.NaN
        if (!peakDbFS.isFinite() || peakDbFS > 0f) return Float.NaN
        return abs(peakDbFS + 89.2f)
    }
}
