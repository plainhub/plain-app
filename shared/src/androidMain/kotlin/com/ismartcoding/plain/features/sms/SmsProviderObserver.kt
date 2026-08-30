package com.ismartcoding.plain.features.sms

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.SmsProviderChangedData
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent

object SmsProviderObserver {
    private const val DEBOUNCE_MILLIS = 500L

    private val handler = Handler(Looper.getMainLooper())
    private val changes = SmsProviderChangeBuffer()
    private var observer: ContentObserver? = null
    private var context: Context? = null

    private val flushChanges = Runnable {
        val uris = synchronized(this) {
            if (observer == null) return@Runnable
            changes.drain().ifEmpty { return@Runnable }
        }
        sendEvent(
            WebSocketEvent(
                EventType.SMS_PROVIDER_CHANGED,
                JsonHelper.jsonEncode(SmsProviderChangedData(uris)),
            ),
        )
    }

    @Synchronized
    fun start(context: Context) {
        if (observer != null) return

        val contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                scheduleChange(uri)
            }
        }
        val resolver = context.applicationContext.contentResolver
        val uris = listOf(
            Telephony.Sms.CONTENT_URI,
            Telephony.Mms.CONTENT_URI,
            Uri.parse("content://mms-sms/conversations"),
        )
        var registrationCount = 0
        uris.forEach { uri ->
            runCatching {
                resolver.registerContentObserver(uri, true, contentObserver)
                registrationCount++
            }.onFailure {
                LogCat.e("SMS provider observer registration failed for $uri: ${it.message}")
            }
        }
        if (registrationCount == 0) return

        this.context = context.applicationContext
        observer = contentObserver
        changes.start()
    }

    @Synchronized
    fun stop() {
        handler.removeCallbacks(flushChanges)
        changes.stop()
        val contentObserver = observer ?: return
        observer = null
        context?.contentResolver?.let { resolver ->
            runCatching { resolver.unregisterContentObserver(contentObserver) }
                .onFailure { LogCat.e("SMS provider observer removal failed: ${it.message}") }
        }
        context = null
    }

    private fun scheduleChange(uri: Uri?) {
        synchronized(this) {
            if (observer == null) return
            if (!changes.add(uri?.toString() ?: "content://mms-sms")) return
            handler.removeCallbacks(flushChanges)
            handler.postDelayed(flushChanges, DEBOUNCE_MILLIS)
        }
    }
}
