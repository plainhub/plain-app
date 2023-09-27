package com.ismartcoding.plain.helpers

import kotlin.math.log10

object SoundMeterHelper {
    fun getMaxAmplitude(
        buffer: ShortArray,
        readSize: Int,
    ): Double {
        return buffer.maxOrNull()?.toDouble() ?: 0.0
    }

    fun amplitudeToDecibel(amplitude: Double): Float {
        return (20 * log10(amplitude)).toFloat()
    }
}
