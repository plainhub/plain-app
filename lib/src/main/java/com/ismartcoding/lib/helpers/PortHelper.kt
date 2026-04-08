package com.ismartcoding.lib.helpers

import com.ismartcoding.lib.logcat.LogCat

object PortHelper {
    fun isPortInUse(port: Int): Boolean {
        return try {
            val socket = java.net.ServerSocket(port)
            socket.close()
            false
        } catch (ex: java.net.BindException) {
            // Port is genuinely occupied by another process.
            true
        } catch (ex: Exception) {
            // Other failures (SecurityException, SELinux denial, etc.)
            // may not indicate an occupied port — log for debugging.
            LogCat.e("Port check for $port failed with ${ex.javaClass.simpleName}: ${ex.message}")
            true
        }
    }
}