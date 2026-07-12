package com.ismartcoding.plain.platform

import com.ismartcoding.plain.web.HttpServerManager

actual fun httpServerPortsInUse(): Set<Int> = HttpServerManager.portsInUse.toSet()
