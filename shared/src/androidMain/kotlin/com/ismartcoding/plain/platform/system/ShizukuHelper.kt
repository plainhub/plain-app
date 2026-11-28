package com.ismartcoding.plain.platform.system

import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

object ShizukuHelper {
    private const val REQUEST_CODE = 10001

    fun isAvailable(): Boolean =
        try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }

    fun isGranted(): Boolean =
        isAvailable() &&
            try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (_: Throwable) {
                false
            }

    fun requestPermission() = Shizuku.requestPermission(REQUEST_CODE)

    fun exec(vararg command: String): String {
        val service = IShizukuService.Stub.asInterface(Shizuku.getBinder())
            ?: throw IllegalStateException("Shizuku service binder is not available")
        val process = service.newProcess(command, null, null)
        // RemoteProcess streams must be read on separate threads: reading
        // stdout to EOF before touching stderr can deadlock when both
        // pipe buffers fill up.
        val stdoutFuture = ioFuture { process.inputStream.readTextAndClose() }
        val stderrText = process.errorStream.readTextAndClose()
        val exit = process.waitFor()
        val stdout = stdoutFuture.get()
        if (exit != 0) {
            throw IllegalStateException("shizuku command failed (exit=$exit): ${stderrText.trim().ifEmpty { stdout.trim() }}")
        }
        return stdout
    }

    private fun ioFuture(block: () -> String): Future<String> =
        CompletableFuture.supplyAsync(block)

    private fun ParcelFileDescriptor.readTextAndClose(): String =
        ParcelFileDescriptor.AutoCloseInputStream(this).use { it.bufferedReader().readText() }
}
