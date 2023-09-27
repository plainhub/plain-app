package com.ismartcoding.plain.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.ismartcoding.lib.isTPlus

class PlugInControlReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == ACTION_USB_STATE) {
            if (intent.extras?.getBoolean("connected") == true) {
            } else {
            }
        }
    }

    companion object {
        const val ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE"

        fun isUSBConnected(context: Context): Boolean {
            val intent =
                if (isTPlus()) {
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
}
