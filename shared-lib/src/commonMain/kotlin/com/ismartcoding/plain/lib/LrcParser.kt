package com.ismartcoding.plain.lib

/**
 * Minimal standard-format LRC parser.
 * Supports multiple time tags per line ([mm:ss], [mm:ss.xx], [mm:ss.xxx]),
 * skips metadata tags ([ti:], [ar:], ...) and strips enhanced-format
 * word tags (<mm:ss.xx>).
 */
object LrcParser {
    data class LrcLine(val timeMs: Long, val text: String)

    private val timeTagRegex = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val wordTagRegex = Regex("""<[^>]*>""")

    fun parse(content: String): List<LrcLine> {
        if (content.isBlank()) return emptyList()
        val lines = mutableListOf<LrcLine>()
        content.lineSequence().forEach { raw ->
            val line = raw.trim()
            val tags = timeTagRegex.findAll(line).toList()
            if (tags.isEmpty()) return@forEach
            val text = wordTagRegex.replace(line.substring(tags.last().range.last + 1), "").trim()
            if (text.isEmpty()) return@forEach
            tags.forEach { tag ->
                val minutes = tag.groupValues[1].toLong()
                val seconds = tag.groupValues[2].toLong()
                val fraction = tag.groupValues[3]
                val fractionMs = when (fraction.length) {
                    0 -> 0L
                    1 -> fraction.toLong() * 100
                    2 -> fraction.toLong() * 10
                    else -> fraction.take(3).toLong()
                }
                lines.add(LrcLine(minutes * 60_000 + seconds * 1_000 + fractionMs, text))
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}