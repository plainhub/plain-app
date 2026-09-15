package com.ismartcoding.plain.helpers

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
            true
        } catch (ex: Exception) {
            LogCat.e("Port check for $port failed with ${ex.javaClass.simpleName}: ${ex.message}")
            true
        }
    }
}