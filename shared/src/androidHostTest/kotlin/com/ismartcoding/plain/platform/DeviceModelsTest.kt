package com.ismartcoding.plain.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CpuInfoTest {

    @Test
    fun cpuUsagePercentDiffsSamples() {
        // 100 ticks total, 25 idle → 75% busy
        val prev = CpuTimes(idle = 100, total = 400)
        val cur = CpuTimes(idle = 125, total = 500)
        assertEquals(75.0, cpuUsagePercent(prev, cur))
    }

    @Test
    fun cpuUsagePercentFullyIdleIsZero() {
        val prev = CpuTimes(idle = 100, total = 400)
        val cur = CpuTimes(idle = 200, total = 500)
        assertEquals(0.0, cpuUsagePercent(prev, cur))
    }

    @Test
    fun cpuUsagePercentSaturatesInsteadOfDividingByZero() {
        // Counters did not advance (sampled too fast): must be 0, not NaN/Inf.
        val prev = CpuTimes(idle = 100, total = 400)
        val cur = CpuTimes(idle = 100, total = 400)
        assertEquals(0.0, cpuUsagePercent(prev, cur))
        // Counter resets (smaller total) must also be 0, never negative.
        assertEquals(0.0, cpuUsagePercent(CpuTimes(500, 900), CpuTimes(10, 100)))
    }

    @Test
    fun cpuUsagePercentClampsToHundred() {
        // Idle went backwards (counter reset mid-diff) — clamp to 100.
        val usage = cpuUsagePercent(CpuTimes(idle = 50, total = 100), CpuTimes(idle = 10, total = 110))
        assertEquals(100.0, usage)
    }

    @Test
    fun parseProcStatParsesRealisticLine() {
        val t = parseProcStatCpuTimes("cpu  74608 2520 24433 1117073 6176 4054 0 0 0 0")
        assertEquals(1117073 + 6176, t!!.idle)
        assertEquals(74608 + 2520 + 24433 + 1117073 + 6176 + 4054, t.total)
    }

    @Test
    fun parseProcStatMinimumFourFields() {
        val t = parseProcStatCpuTimes("cpu  1 2 3 4")
        assertEquals(4, t!!.idle)
        assertEquals(10, t.total)
    }

    @Test
    fun parseProcStatRejectsMalformed() {
        assertNull(parseProcStatCpuTimes("cpu  1 2 3"))
        assertNull(parseProcStatCpuTimes("cpu  1 2 3 x 5"))
        assertNull(parseProcStatCpuTimes(""))
        assertNull(parseProcStatCpuTimes("meminfo: 42"))
        assertNull(parseProcStatCpuTimes("cpux 1 2 3 4"))
    }

    @Test
    fun parseCpuInfoModelExtractsX86StyleModel() {
        val content = """
            processor	: 0
            vendor_id	: GenuineIntel
            model name	: Intel(R) Core(TM) i7-9750H CPU @ 2.60GHz
            processor	: 1
            model name	: Intel(R) Core(TM) i7-9750H CPU @ 2.60GHz
        """.trimIndent()
        assertEquals("Intel(R) Core(TM) i7-9750H CPU @ 2.60GHz", parseCpuInfoModel(content))
        assertEquals("", parseCpuInfoModel("CPU part\t: 0x805\n"))
    }
}
