package com.ismartcoding.plain.mdns

import com.ismartcoding.plain.discover.RustMdnsRuntime
import kotlinx.serialization.json.*

object NsdHelper {
    suspend fun registerServices(): Boolean = RustMdnsRuntime.control("publish").getValue("running").jsonPrimitive.boolean
    fun unregisterService() = RustMdnsRuntime.request("unpublish")
}
