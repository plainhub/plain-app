package com.ismartcoding.plain.platform

/**
 * Aggregated CPU tick counters of one sampling instant. Platform samplers
 * fill it from /proc/stat (Android) or mach (iOS); [cpuUsagePercent] diffs
 * two samples into a usage percentage.
 */
class CpuTimes(
    val idle: Long,
    val total: Long,
)

/**
 * Diff two CPU counter samples into a 0-100 usage percentage.
 * Returns 0 when the counters did not advance (sampled too fast, or the
 * platform reported no data) instead of dividing by zero.
 */
fun cpuUsagePercent(prev: CpuTimes, cur: CpuTimes): Double {
    val idleDelta = cur.idle - prev.idle
    val totalDelta = cur.total - prev.total
    if (totalDelta <= 0) return 0.0
    val usage = (1.0 - idleDelta.toDouble() / totalDelta) * 100.0
    return usage.coerceIn(0.0, 100.0)
}

/**
 * Parse the aggregate `cpu  user nice system idle iowait irq softirq steal ...`
 * line from /proc/stat. idle = idle + iowait; total = sum of all fields.
 * Returns null for malformed input (missing fields, non-numeric).
 */
fun parseProcStatCpuTimes(line: String): CpuTimes? {
    val parts = line.trim().split(Regex("\\s+"))
    if (parts.isEmpty() || parts[0] != "cpu") return null
    val values = parts.drop(1).map { it.toLongOrNull() ?: return null }
    if (values.size < 4) return null
    val idle = values[3] + (values.getOrElse(4) { 0L })
    val total = values.sum()
    return CpuTimes(idle, total)
}

/**
 * Extract the first `model name` value from /proc/cpuinfo content.
 * x86 kernels expose it per-processor; many ARM kernels omit it entirely,
 * in which case this returns "" and callers fall back to another source.
 */
fun parseCpuInfoModel(content: String): String {
    content.lineSequence().forEach { line ->
        val idx = line.indexOf(':')
        if (idx > 0) {
            val key = line.substring(0, idx).trim()
            if (key == "model name") {
                return line.substring(idx + 1).trim()
            }
        }
    }
    return ""
}
