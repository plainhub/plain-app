package com.ismartcoding.plain.platform

import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku

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

    /**
     * Fire-and-forget permission request. The system dialog is shown by the
     * Shizuku app; the caller should poll [isGranted] afterwards.
     */
    fun requestPermission() {
        if (!isAvailable() || isGranted()) return
        try {
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (_: Throwable) {
        }
    }

    /**
     * Run a shell command as the shell uid via Shizuku.
     * The shell uid already carries the WRITE_SMS appop, so Telephony
     * provider deletes are permitted without any extra appops setup.
     */
    fun exec(command: String): String {
        val service = IShizukuService.Stub.asInterface(Shizuku.getBinder())
            ?: throw IllegalStateException("Shizuku service binder is not available")
        val process = service.newProcess(arrayOf("sh", "-c", command), null, null)
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

    private fun ioFuture(block: () -> String): java.util.concurrent.Future<String> =
        java.util.concurrent.CompletableFuture.supplyAsync(block)

    private fun ParcelFileDescriptor.readTextAndClose(): String =
        ParcelFileDescriptor.AutoCloseInputStream(this).use { it.bufferedReader().readText() }
}
