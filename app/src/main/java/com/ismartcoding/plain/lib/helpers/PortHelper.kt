package com.ismartcoding.plain.lib.helpers

import com.ismartcoding.plain.lib.logcat.LogCat
import java.net.BindException
import java.net.ServerSocket

object PortHelper {
    fun isPortInUse(port: Int): Boolean {
        return try {
            val socket = ServerSocket(port)
            socket.close()
            false
        } catch (ex: BindException) {
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