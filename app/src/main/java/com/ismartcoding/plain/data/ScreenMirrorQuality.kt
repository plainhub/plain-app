package com.ismartcoding.plain.data

data class ScreenMirrorQuality(val maxSize: Long, val quality: Int, val maxWidth: Int) {
    companion object {
        val LOW = ScreenMirrorQuality(
            30720,// 30KB
            30, 480
        )
        val HIGH = ScreenMirrorQuality(
            102400, // 100KB,
            70, 720
        )
    }
}