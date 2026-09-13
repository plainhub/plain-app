package com.ismartcoding.plain.lib

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LrcWriterTest {
    @Test
    fun writesStandardTimeTags() {
        val text =
            LrcWriter.write(
                listOf(
                    LrcParser.LrcLine(12500, "first"),
                    LrcParser.LrcLine(61599, "second"),
                ),
            )
        assertEquals("[00:12.50]first\n[01:01.59]second", text)
    }

    @Test
    fun sortsLinesAndDropsBlankText() {
        val text =
            LrcWriter.write(
                listOf(
                    LrcParser.LrcLine(2000, "b"),
                    LrcParser.LrcLine(1000, " "),
                    LrcParser.LrcLine(3000, "a"),
                ),
            )
        assertEquals("[00:02.00]b\n[00:03.00]a", text)
    }

    @Test
    fun outputRoundTripsThroughParser() {
        // LRC centisecond tags lose sub-10ms precision; use aligned inputs.
        val lines = listOf(LrcParser.LrcLine(61590, "成都"), LrcParser.LrcLine(12540, "let it go"))
        val parsed = LrcParser.parse(LrcWriter.write(lines))
        assertEquals(lines.sortedBy { it.timeMs }, parsed)
    }
}

class TranscribeMergeTest {
    private fun seg(start: Long, text: String = "x") = RawSegment(start, start + 1000, text)

    @Test
    fun shiftMovesSegmentsOntoSongTimeline() {
        val shifted = TranscribeMerge.shift(listOf(seg(1000), seg(5000)), 88_000)
        assertEquals(listOf(seg(89_000), seg(93_000)), shifted)
    }

    @Test
    fun dropOverlapRemovesSegmentsInsidePreviousChunk() {
        val segments = listOf(seg(500), seg(2500))
        assertEquals(listOf(seg(2500)), TranscribeMerge.dropOverlap(segments, 2000))
        assertEquals(segments, TranscribeMerge.dropOverlap(segments, 0))
    }

    @Test
    fun toLrcLinesTrimsAndSkipsBlank() {
        val lines = TranscribeMerge.toLrcLines(listOf(RawSegment(1000, 2000, "  hi  "), RawSegment(3000, 4000, "  ")))
        assertEquals(listOf(LrcParser.LrcLine(1000, "hi")), lines)
    }
}

class AudioResamplerTest {
    @Test
    fun constantInputStaysConstant() {
        val resampler = AudioResampler(44100, 16000)
        val input = FloatArray(44100) { 0.5f }
        val out = mutableListOf<Float>()
        resampler.process(input, 0, input.size) { samples, n -> repeat(n) { out.add(samples[it]) } }
        resampler.flush { samples, n -> repeat(n) { out.add(samples[it]) } }
        // ~16000 output samples for 1s of input
        assertTrue(out.size in 15500..16500, "size=${out.size}")
        // Skip the windowed-sinc ramp-in/out at both ends.
        val stable = out.subList(out.size / 10, out.size * 9 / 10)
        assertTrue(stable.min() >= 0.49f, "min=${stable.min()}")
        assertTrue(stable.max() <= 0.51f, "max=${stable.max()}")
    }

    @Test
    fun preservesSineFrequencyWithinTolerance() {
        val inRate = 44100
        val outRate = 16000
        val freq = 440.0
        val resampler = AudioResampler(inRate, outRate)
        val out = mutableListOf<Float>()
        val seconds = 0.5
        val input = FloatArray((inRate * seconds).toInt()) { sin(2 * PI * freq * it / inRate).toFloat() }
        resampler.process(input, 0, input.size) { samples, n -> repeat(n) { out.add(samples[it]) } }
        resampler.flush { samples, n -> repeat(n) { out.add(samples[it]) } }
        // Zero-crossing frequency estimate on the stable middle section.
        val start = out.size / 5
        val end = out.size * 4 / 5
        var crossings = 0
        for (i in start + 1 until end) {
            if (out[i - 1] < 0 && out[i] >= 0) crossings++
        }
        val duration = (end - start).toDouble() / outRate
        val estimated = crossings / duration
        assertTrue(kotlin.math.abs(estimated - freq) < freq * 0.05, "estimated=$estimated")
    }
}
