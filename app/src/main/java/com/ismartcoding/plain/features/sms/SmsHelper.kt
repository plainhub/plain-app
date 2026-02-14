package com.ismartcoding.plain.features.sms

import android.content.Context
import android.os.Build
import android.telephony.SmsManager

object SmsHelper {
    fun sendText(context: Context, number: String, body: String) {
        val to = number.trim()
        val message = body.trim()

        require(to.isNotEmpty()) { "Phone number is required" }
        require(message.isNotEmpty()) { "Message body is required" }

        val manager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            SmsManager.getDefault()
        }

        val parts = manager.divideMessage(message)
        if (parts.size > 1) {
            manager.sendMultipartTextMessage(to, null, ArrayList(parts), null, null)
        } else {
            manager.sendTextMessage(to, null, message, null, null)
        }
    }
}
