package com.ismartcoding.plain.discover

import com.ismartcoding.plain.platform.PlatformLock
import kotlinx.serialization.json.*

internal expect fun setMdnsMulticastPermission(acquire: Boolean): Boolean

object MdnsMulticastHost {
    private val lock = PlatformLock()
    private var active: String? = null
    private val closed = linkedSetOf<String>()

    fun handle(params: JsonObject): JsonPrimitive = lock.withLock {
        val lease = params.getValue("lease").jsonPrimitive.content
        require(lease.isNotEmpty() && lease.length <= 128)
        val acquire = params.getValue("acquire").jsonPrimitive.boolean
        if (acquire) {
            if (lease in closed || active?.let { it != lease } == true) return@withLock JsonPrimitive(false)
            val acquired = setMdnsMulticastPermission(true)
            if (acquired) active = lease
            JsonPrimitive(acquired)
        } else {
            closed.add(lease)
            while (closed.size > 128) closed.remove(closed.first())
            if (active != lease) return@withLock JsonPrimitive(true)
            val released = setMdnsMulticastPermission(false)
            if (released) active = null
            JsonPrimitive(released)
        }
    }
    fun disconnect() = lock.withLock {
        setMdnsMulticastPermission(false)
        active = null
    }
}
