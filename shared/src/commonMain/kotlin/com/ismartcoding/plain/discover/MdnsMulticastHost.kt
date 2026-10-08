package com.ismartcoding.plain.discover

import com.ismartcoding.plain.platform.PlatformLock
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*

internal expect fun setMdnsMulticastPermission(acquire: Boolean): Boolean

object MdnsMulticastHost {
    private val lock = PlatformLock()
    private val active = linkedSetOf<String>()
    private val closed = linkedSetOf<String>()

    fun handle(params: JsonObject): JsonPrimitive = lock.withLock {
        val request = JsonHelper.jsonDecodeFromElement<MulticastPermissionRequest>(params)
        val lease = request.lease
        require(lease.isNotEmpty() && lease.length <= 128)
        val acquire = request.acquire
        if (acquire) {
            if (lease in closed) return@withLock JsonPrimitive(false)
            val acquired = setMdnsMulticastPermission(true)
            if (acquired) active.add(lease)
            JsonPrimitive(acquired)
        } else {
            closed.add(lease)
            while (closed.size > 128) closed.remove(closed.first())
            if (!active.remove(lease) || active.isNotEmpty()) return@withLock JsonPrimitive(true)
            val released = setMdnsMulticastPermission(false)
            if (!released) active.add(lease)
            JsonPrimitive(released)
        }
    }
    fun disconnect() = lock.withLock {
        setMdnsMulticastPermission(false)
        active.clear()
    }
}
