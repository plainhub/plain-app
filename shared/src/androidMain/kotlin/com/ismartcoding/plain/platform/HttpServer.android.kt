package com.ismartcoding.plain.platform

import android.content.Intent
import androidx.core.content.ContextCompat
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.services.HttpServerService
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.webserver.checkServerHealthAsync
import com.ismartcoding.plain.webserver.generateSslKeyStoreFile
import com.ismartcoding.plain.webserver.getSslSignatureBytes
import com.ismartcoding.plain.webserver.stopHttpServiceAsync
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

actual fun httpServerPortsInUse(): Set<Int> = HttpServerManager.portsInUse.toSet()

actual fun getSSLSignature(password: String): ByteArray =
    getSslSignatureBytes(appContext, password)

actual fun generateSSLKeyStore(password: String) {
    generateSslKeyStoreFile(File(appContext.filesDir, Constants.KEY_STORE_FILE_NAME), password)
}

actual suspend fun resetPasswordAsync(): String = HttpServerManager.resetPasswordAsync()

actual suspend fun checkHttpServerAsync(): Boolean = checkServerHealthAsync()

actual suspend fun disposeHttpServer() {
    kotlinx.coroutines.delay(100)
    com.ismartcoding.plain.webserver.httpServer?.stop(500, 1000)
    com.ismartcoding.plain.webserver.httpServer = null
}

actual suspend fun stopHttpServiceAsync() {
    com.ismartcoding.plain.webserver.stopHttpServiceAsync(appContext)
}

actual fun startHttpServerService() {
    coIO {
        var retry = 3
        val context = appContext
        while (retry > 0) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, HttpServerService::class.java),
                )
                break
            } catch (ex: Exception) {
                LogCat.e(ex.toString())
                kotlinx.coroutines.delay(500.milliseconds)
                retry--
            }
        }
    }
}
