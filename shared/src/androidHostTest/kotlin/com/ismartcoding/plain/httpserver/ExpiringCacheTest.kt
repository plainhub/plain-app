package com.ismartcoding.plain.httpserver

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ExpiringCacheTest {
    private class FakeClock(var nowMs: Long = 0L) {
        fun advance(ms: Long) {
            nowMs += ms
        }
    }

    private val posTtl = 1000L
    private val negTtl = 100L

    @Test
    fun get_miss_callsLoadOnce_thenHit() = runBlocking {
        var loads = 0
        val clock = FakeClock()
        val cache = ExpiringCache<String, String>(
            positiveTtlMillis = posTtl,
            negativeTtlMillis = negTtl,
            load = { key: String ->
                loads++
                "v-$key"
            },
            nowMillis = { clock.nowMs },
        )
        assertEquals("v-a", cache.get("a"))
        assertEquals("v-a", cache.get("a"))
        assertEquals(1, loads)
    }

    @Test
    fun get_afterPut_hitsWithoutLoad() = runBlocking {
        var loads = 0
        val clock = FakeClock()
        val cache = ExpiringCache<String, String>(
            positiveTtlMillis = posTtl,
            negativeTtlMillis = negTtl,
            load = {
                loads++
                "loaded"
            },
            nowMillis = { clock.nowMs },
        )
        cache.put("a", "cached")
        assertEquals("cached", cache.get("a"))
        assertEquals(0, loads)
    }

    @Test
    fun get_positiveExpiry_reloads() = runBlocking {
        var loads = 0
        val clock = FakeClock()
        val cache = ExpiringCache<String, String>(
            positiveTtlMillis = posTtl,
            negativeTtlMillis = negTtl,
            load = { key: String ->
                loads++
                "v-$key"
            },
            nowMillis = { clock.nowMs },
        )
        assertEquals("v-a", cache.get("a"))
        clock.advance(1500)
        assertEquals("v-a", cache.get("a"))
        assertEquals(2, loads)
    }

    @Test
    fun get_putRefreshesTtl() = runBlocking {
        var loads = 0
        val clock = FakeClock()
        val cache = ExpiringCache<String, String>(
            positiveTtlMillis = posTtl,
            negativeTtlMillis = negTtl,
            load = {
                loads++
                "loaded"
            },
            nowMillis = { clock.nowMs },
        )
        cache.put("a", "cached")
        clock.advance(900) // 未到 TTL，命中且不重载
        assertEquals("cached", cache.get("a"))
        assertEquals(0, loads)
        clock.advance(200) // 累计 1100 > TTL，put 的缓存已过期，触发重载
        assertEquals("loaded", cache.get("a"))
        assertTrue(loads >= 1)
    }

    @Test
    fun get_negativeCaching_queriesDbOnceWithinTtl() = runBlocking {
        var loads = 0
        val clock = FakeClock()
        val cache = ExpiringCache<String, String>(
            positiveTtlMillis = posTtl,
            negativeTtlMillis = negTtl,
            load = {
                loads++
                null
            },
            nowMillis = { clock.nowMs },
        )
        assertNull(cache.get("x"))
        assertNull(cache.get("x")) // 负缓存窗口内，不重复加载
        assertEquals(1, loads)
        clock.advance(negTtl + 1)
        assertNull(cache.get("x")) // 负缓存过期，重新加载
        assertEquals(2, loads)
    }

    @Test
    fun get_pureCache_noLoad_noBackfill() = runBlocking {
        val clock = FakeClock()
        val cache = ExpiringCache<String, String>(
            positiveTtlMillis = posTtl,
            negativeTtlMillis = negTtl,
            load = null,
            nowMillis = { clock.nowMs },
        )
        assertNull(cache.get("a")) // 无 put、无 load，直接 null，也不做负缓存
        cache.put("a", "ip")
        assertEquals("ip", cache.get("a"))
        clock.advance(1500) // TTL 过期后纯缓存清空，返回 null
        assertNull(cache.get("a"))
    }

    @Test
    fun invalidate_dropsKey_andReloads() = runBlocking {
        var loads = 0
        val clock = FakeClock()
        val cache = ExpiringCache<String, String>(
            positiveTtlMillis = posTtl,
            negativeTtlMillis = negTtl,
            load = { key: String ->
                loads++
                "v-$key"
            },
            nowMillis = { clock.nowMs },
        )
        cache.put("a", "cached")
        cache.invalidate("a")
        assertEquals("v-a", cache.get("a"))
        assertEquals(1, loads)
    }

    @Test
    fun getCachedOrNull_returnsOnlyFromMemory() {
        val clock = FakeClock()
        val cache = ExpiringCache<String, String>(
            positiveTtlMillis = posTtl,
            negativeTtlMillis = negTtl,
            load = { "loaded" },
            nowMillis = { clock.nowMs },
        )
        assertNull(cache.getCachedOrNull("a")) // 未 put，非挂起读取返回 null，不回源
    }

    @Test
    fun concurrentAccess_isSafe() = runBlocking {
        val clock = FakeClock()
        val cache = ExpiringCache<String, Int>(
            positiveTtlMillis = 100_000,
            negativeTtlMillis = 100,
            load = { key: String -> key.length },
            nowMillis = { clock.nowMs },
        )
        val jobs = (1..64).map {
            launch {
                repeat(200) {
                    val v = cache.get("abc")
                    assertEquals(3, v)
                    cache.put("abc", requireNotNull(v))
                    cache.invalidate("abc")
                }
            }
        }
        jobs.joinAll()
        // 并发读写后缓存仍可用且一致
        assertEquals(3, cache.get("abc"))
    }
}