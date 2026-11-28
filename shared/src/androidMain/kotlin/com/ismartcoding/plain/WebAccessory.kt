package com.ismartcoding.plain

import android.app.PendingIntent
import android.content.Intent
import android.hardware.usb.UsbManager
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.rust.native.RustNativeBindings
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

private val serving = AtomicBoolean(false)

internal fun MainActivity.openWebAccessory(intent: Intent) {
    val permissionResult = "$packageName.USB_ACCESSORY_PERMISSION"
    if (intent.action !in setOf(Intent.ACTION_MAIN, UsbManager.ACTION_USB_ACCESSORY_ATTACHED, permissionResult)) return
    val manager = getSystemService(UsbManager::class.java)
    val accessory = manager.accessoryList?.singleOrNull() ?: return
    if (accessory.manufacturer != "PlainApp" || accessory.model != "Web" ||
        accessory.version != "1") return
    if (!manager.hasPermission(accessory)) {
        if (intent.action == Intent.ACTION_MAIN) {
            val result = Intent(this, MainActivity::class.java).setAction(permissionResult)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            manager.requestPermission(accessory, PendingIntent.getActivity(this, 0, result, PendingIntent.FLAG_IMMUTABLE))
        }
        return
    }
    if (!serving.compareAndSet(false, true)) return
    thread(name = "PlainApp WebUSB") {
        try {
            manager.openAccessory(accessory)?.use { RustNativeBindings.serveWebAccessory(it.fd) }
        } catch (e: Exception) {
            LogCat.e("WebUSB connection closed: ${e.message}")
        } finally {
            serving.set(false)
        }
    }
}
