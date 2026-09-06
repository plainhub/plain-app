package com.ismartcoding.plain.features.sms

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.provider.Telephony
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.SmsProviderChangedData
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SmsProviderObserver {
    private const val DEBOUNCE_MILLIS = 500L

    // Nothing here touches the UI, and the flush (JSON encode + WebSocket
    // broadcast) must stay off the main thread even during SMS restore storms.
    // A plain Default scope also keeps this object free of looper/framework
    // dependencies, so server stop hooks can touch it in any environment.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val changes = SmsProviderChangeBuffer()
    private var observer: ContentObserver? = null
    private var context: Context? = null
    private var flushJob: Job? = null

    @Synchronized
    fun start(context: Context) {
        if (observer != null) return

        // A null handler dispatches onChange on the calling binder thread; the
        // body only buffers and re-arms the debounce, both under [this] monitor.
        val contentObserver = object : ContentObserver(null) {
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
        val contentObserver = observer ?: return
        flushJob?.cancel()
        flushJob = null
        changes.stop()
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
            // Debounce: every change in a burst resets the pending flush.
            flushJob?.cancel()
            flushJob = scope.launch {
                delay(DEBOUNCE_MILLIS)
                val uris = synchronized(this@SmsProviderObserver) {
                    if (observer == null) return@launch
                    changes.drain().ifEmpty { return@launch }
                }
                sendEvent(
                    WebSocketEvent(
                        EventType.SMS_PROVIDER_CHANGED,
                        JsonHelper.jsonEncode(SmsProviderChangedData(uris)),
                    ),
                )
            }
        }
    }
}
