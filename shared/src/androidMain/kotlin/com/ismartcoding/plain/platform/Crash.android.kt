package com.ismartcoding.plain.platform

import com.ismartcoding.plain.CrashHandler
import com.ismartcoding.plain.appContext

actual fun getAppLogs(): String = CrashHandler.getAppLogs(appContext)
