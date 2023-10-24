package com.ismartcoding.lib.helpers

object PortHelper {
    fun isPortInUse(port: Int): Boolean {
        return try {
            val socket = java.net.ServerSocket(port)
            socket.close()
            false
        } catch (ex: Exception) {
            true
        }
    }
}