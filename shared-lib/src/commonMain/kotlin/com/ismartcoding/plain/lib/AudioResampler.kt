package com.ismartcoding.plain.lib

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Streaming windowed-sinc resampler for mono float PCM. Push arbitrary-sized
 * input blocks via [process]; resampled blocks come back through the output
 * callback. A small tail of input samples is kept across calls.
 */
class AudioResampler(private val inRate: Int, private val outRate: Int) {
    init {
        require(inRate > 0 && outRate > 0) { "invalid sample rates: $inRate -> $outRate" }
    }

    private val ratio = inRate.toDouble() / outRate
    private val halfTaps = 16
    // Normalized cutoff (cycles per input sample), at or below the Nyquist of the lower rate.
    private val cutoff = 0.5 * (if (outRate < inRate) outRate.toDouble() / inRate else 1.0)
    private var history = FloatArray(0)
    private var pos = halfTaps.toDouble()

    fun process(
        input: FloatArray,
        offset: Int,
        length: Int,
        output: (samples: FloatArray, count: Int) -> Unit,
    ) {
        val buf = FloatArray(history.size + length)
        history.copyInto(buf)
        input.copyInto(buf, history.size, offset, offset + length)
        val capacity = if (buf.size > halfTaps + 1) ((buf.size - halfTaps - 1) / ratio).toInt() + 1 else 1
        val out = FloatArray(capacity)
        var outCount = 0
        while (pos + halfTaps <= buf.size - 1) {
            val base = pos.toInt()
            var acc = 0.0
            for (k in -halfTaps..halfTaps) {
                val idx = base + k
                val sample = buf.getOrNull(idx) ?: continue
                acc += sample * kernel(pos - idx)
            }
            if (outCount >= out.size) break
            out[outCount++] = acc.toFloat()
            pos += ratio
        }
        val keepFrom = (pos - halfTaps).toInt().coerceIn(0, buf.size)
        history = buf.copyOfRange(keepFrom, buf.size)
        pos -= keepFrom
        if (outCount > 0) output(out, outCount)
    }

    /** Flushes the tail after the input stream ends (zero-padding the remainder). */
    fun flush(output: (samples: FloatArray, count: Int) -> Unit) {
        val tail = FloatArray(halfTaps * 2 + 1)
        process(tail, 0, tail.size, output)
    }

    private fun kernel(x: Double): Double {
        // h(x) = sin(2*pi*f*x)/(pi*x) with a Hann window over [-halfTaps, halfTaps].
        if (abs(x) < 1e-9) return 2.0 * cutoff
        val window = 0.5 * (1.0 + cos(PI * x / halfTaps))
        return sin(2.0 * PI * cutoff * x) / (PI * x) * window
    }
}
