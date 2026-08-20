package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 进程内 TTL 过期缓存，支持按需加载（可关闭）与负缓存。
 *
 * [load] 可为 null 表示**纯缓存**：值只由 [put] 写入（如运行时从 socket
 * 拿到的 client IP），[get] 未命中直接返回 null，不回源、不做负缓存。
 * [load] 非空则表示值来自数据源（如 DB），未命中/过期时按需加载并回填。
 *
 * 并发模型：所有 map 读写经单个 Mutex 串行化（single-writer）。加载在锁外
 * 执行，因此同一刚失效 key 的并发请求可能重复加载——可接受，因为加载廉价
 * 且 map 始终一致。不用 Compose SnapshotState：本缓存属于非 UI 的 HTTP 层
 * 数据结构，普通线程安全容器即可。
 */
class ExpiringCache<K, V>(
    private val positiveTtlMillis: Long,
    private val negativeTtlMillis: Long,
    private val load: (suspend (K) -> V?)? = null,
    private val nowMillis: () -> Long = { TimeHelper.nowMillis() },
) {
    private class Entry<V>(val value: V?, val loadedAtMillis: Long)

    private val entries = HashMap<K, Entry<V>>()
    private val mutex = Mutex()

    /** 命中返回；未命中或过期则调用 [load] 加载并回填（纯缓存时返回 null）。 */
    suspend fun get(key: K): V? {
        val now = nowMillis()
        mutex.withLock {
            val entry = entries[key]
            if (entry != null) {
                val ttl =
                    if (entry.value == null) negativeTtlMillis
                    else positiveTtlMillis
                if (now - entry.loadedAtMillis < ttl) return entry.value
            }
        }
        val value = load?.invoke(key) // 锁外加载，允许并发重复加载；纯缓存时为 null
        if (value != null || load != null) {
            val ttl = if (value == null) negativeTtlMillis else positiveTtlMillis
            mutex.withLock { entries[key] = Entry(value, now) }
        }
        return value
    }

    /** 写入运行时才知道的值（socket IP、登录产生的 token），刷新 TTL。 */
    suspend fun put(key: K, value: V) {
        mutex.withLock { entries[key] = Entry(value, nowMillis()) }
    }

    /** 立即移除某键（底层会话被删除 / token 重置时调用）。 */
    suspend fun invalidate(key: K) {
        mutex.withLock { entries.remove(key) }
    }

    /**
     * 非挂起读取：仅在内存中查询未过期的缓存值，不回源。供 UI 组合期这类
     * 不能挂起的场景展示用（如登录确认页显示 client IP）。
     */
    fun getCachedOrNull(key: K): V? {
        if (!mutex.tryLock()) return null
        try {
            val entry = entries[key] ?: return null
            val ttl = if (entry.value == null) negativeTtlMillis else positiveTtlMillis
            return if (nowMillis() - entry.loadedAtMillis < ttl) entry.value else null
        } finally {
            mutex.unlock()
        }
    }

    suspend fun clear() {
        mutex.withLock { entries.clear() }
    }
}