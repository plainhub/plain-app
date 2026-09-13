package com.ismartcoding.plain.lib

/** Serializes lyric lines to standard LRC text with [mm:ss.xx] time tags. */
object LrcWriter {
    fun write(lines: List<LrcParser.LrcLine>): String {
        return lines
            .filter { it.text.isNotBlank() }
            .sortedBy { it.timeMs }
            .joinToString("\n") { line ->
                val totalSeconds = line.timeMs / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                val hundredths = (line.timeMs % 1000) / 10
                val m = minutes.toString().padStart(2, '0')
                val s = seconds.toString().padStart(2, '0')
                val h = hundredths.toString().padStart(2, '0')
                "[$m:$s.$h]${line.text}"
            }
    }
}
