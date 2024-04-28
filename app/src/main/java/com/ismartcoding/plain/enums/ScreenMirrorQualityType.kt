package com.ismartcoding.plain.enums

import com.ismartcoding.plain.data.ScreenMirrorQuality

enum class ScreenMirrorQualityType(val value: Int) {
    LOW(1),
    HIGH(2);

    fun getQuality(): ScreenMirrorQuality {
        return when (this) {
            LOW -> ScreenMirrorQuality.LOW
            HIGH -> ScreenMirrorQuality.HIGH
        }
    }
}