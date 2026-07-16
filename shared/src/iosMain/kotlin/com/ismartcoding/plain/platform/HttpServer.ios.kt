package com.ismartcoding.plain.platform

actual fun httpServerPortsInUse(): Set<Int> = emptySet()

actual fun getSSLSignature(password: String): ByteArray = ByteArray(0)

actual fun generateSSLKeyStore(password: String) {}

actual suspend fun resetPasswordAsync(): String = ""

actual suspend fun checkHttpServerAsync(): Boolean = false

actual suspend fun disposeHttpServer() {}

actual suspend fun stopHttpServiceAsync() {}

actual fun startHttpServerService() {}
