package com.ismartcoding.plain.receivers

import android.content.Context
import android.content.IntentFilter
import com.ismartcoding.lib.isTPlus

object PlugInControlReceiver {
    private const val ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE"

    fun isUSBConnected(context: Context): Boolean {
        val intent = if (isTPlus()) {
            context.registerReceiver(
                null,
                IntentFilter(ACTION_USB_STATE),
                Context.RECEIVER_NOT_EXPORTED,
            )
        } else {
            context.registerReceiver(null, IntentFilter(ACTION_USB_STATE))
        }
        return intent?.extras?.getBoolean("connected") == true
    }
}
