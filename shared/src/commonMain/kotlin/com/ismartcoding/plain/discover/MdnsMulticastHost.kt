package com.ismartcoding.plain.discover

import kotlinx.serialization.json.*

internal expect fun setMdnsMulticastPermission(acquire: Boolean): Boolean

object MdnsMulticastHost {
    fun handle(params: JsonObject): JsonPrimitive =
        JsonPrimitive(setMdnsMulticastPermission(params.getValue("acquire").jsonPrimitive.boolean))

    fun disconnect() {
        setMdnsMulticastPermission(false)
    }
}
