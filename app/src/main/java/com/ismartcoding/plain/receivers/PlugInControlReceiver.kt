package com.ismartcoding.plain.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.isTPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.events.PowerConnectedEvent
import com.ismartcoding.plain.events.PowerDisconnectedEvent

class PlugInControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_POWER_CONNECTED) {
            LogCat.d("PlugInControlReceiver: ACTION_POWER_CONNECTED")
            sendEvent(PowerConnectedEvent())
        } else if (action == Intent.ACTION_POWER_DISCONNECTED) {
            LogCat.d("PlugInControlReceiver: ACTION_POWER_DISCONNECTED")
            sendEvent(PowerDisconnectedEvent())
        }
    }

    companion object {
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
}
