package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.mdns.MdnsRegister
import com.ismartcoding.plain.mdns.NsdHelper
import com.ismartcoding.plain.discover.RustMdnsRuntime

internal object HttpServerResources {
    private var mdnsRegister: MdnsRegister? = null
    suspend fun start() {
        if (mdnsRegister == null) mdnsRegister = MdnsRegister(appContext).also { it.start() }
        NsdHelper.registerServices()
    }
    fun schedule(reason: String) { mdnsRegister?.schedule(reason) }
    suspend fun stop() {
        mdnsRegister?.stop()
        mdnsRegister = null
        RustMdnsRuntime.control("unpublish")
    }
}
