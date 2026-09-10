package com.ismartcoding.plain.features

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.db.DClipboard
import com.ismartcoding.plain.events.ClipboardChangedData
import com.ismartcoding.plain.events.ClipboardSyncChangedEvent
import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.crypto.sha256
import com.ismartcoding.plain.lib.generateId
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.preferences.ClipboardSyncPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Watches the system clipboard while the HTTP server service is alive.
 *
 * Focused reads (app in foreground, or our transparent floating activity):
 * the primary-clip listener captures changes, gates on [ClipboardSyncPreference],
 * dedups by content hash (also suppresses loops from desktop writes), persists
 * to the clipboard history table and broadcasts CLIPBOARD_CHANGED(39).
 *
 * Background reads are blocked on Android 10+ unless the app has focus. Same
 * workaround as KDE Connect: with READ_LOGS (adb-granted) a logcat thread watches
 * the system ClipboardService denial log for our package name and launches
 * [ClipboardFloatingActivity], which briefly takes focus so the read succeeds.
 * Without READ_LOGS, sync falls back to "when the app is next focused".
 */
object ClipboardWatcher {
    private const val MAX_TEXT_LENGTH = 256 * 1024

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var eventJob: Job? = null
    private var logcatJob: Job? = null
    private var logcatProcess: Process? = null

    fun canSyncAutomatically(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_LOGS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    @Synchronized
    fun start() {
        if (eventJob != null) return
        try {
            val l = ClipboardManager.OnPrimaryClipChangedListener { handleChange() }
            clipboardManager.addPrimaryClipChangedListener(l)
            listener = l
        } catch (e: Exception) {
            LogCat.e("ClipboardWatcher start failed: ${e.message}")
        }
        eventJob = coIO {
            if (ClipboardSyncPreference.getAsync()) startLogcat()
            Channel.sharedFlow.collect { event ->
                when (event) {
                    is ClipboardSyncChangedEvent ->
                        if (event.enabled && canSyncAutomatically(appContext)) startLogcat() else stopLogcat()
                    // Background clip changes were denied at read time (Android 10+);
                    // re-read once our own window regains focus so the history catches up.
                    is WindowFocusChangedEvent -> if (event.hasFocus) handleFocusGained()
                    else -> Unit
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        listener?.let {
            runCatching { clipboardManager.removePrimaryClipChangedListener(it) }
                .onFailure { LogCat.e("ClipboardWatcher stop failed: ${it.message}") }
        }
        listener = null
        stopLogcat()
        eventJob?.cancel()
        eventJob = null
    }

    /** Called when one of our windows gains focus (main activity / floating activity). */
    fun handleFocusGained() {
        scope.launch {
            try {
                handleClipboardChange()
            } catch (e: Exception) {
                LogCat.e("ClipboardWatcher focus read failed: ${e.message}")
            }
        }
    }

    @Synchronized
    private fun startLogcat() {
        if (logcatJob?.isActive == true || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        if (!canSyncAutomatically(appContext)) return
        try {
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            val filter = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                "E ClipboardService"
            } else {
                "ClipboardService:E"
            }
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-T", timeStamp, filter, "*:S"))
            logcatProcess = process
            logcatJob = scope.launch {
                BufferedReader(InputStreamReader(process.inputStream)).forEachLine { line ->
                    if (line.contains(appContext.packageName)) {
                        appContext.startActivity(
                            Intent(appContext, ClipboardFloatingActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            LogCat.e("ClipboardWatcher logcat failed: ${e.message}")
        }
    }

    @Synchronized
    private fun stopLogcat() {
        logcatJob?.cancel()
        logcatJob = null
        logcatProcess?.destroy()
        logcatProcess = null
    }

    private fun handleChange() {
        scope.launch {
            try {
                handleClipboardChange()
            } catch (e: Exception) {
                LogCat.e("ClipboardWatcher handle failed: ${e.message}")
            }
        }
    }

    private suspend fun handleClipboardChange() {
        if (!ClipboardSyncPreference.getAsync()) return
        val clip = clipboardManager.primaryClip ?: return
        val text = clip.getItemAt(0).coerceToText(appContext)?.toString() ?: return
        if (text.isBlank()) return
        if (text.length > MAX_TEXT_LENGTH) {
            LogCat.d("ClipboardWatcher: clip too large (${text.length} chars), skipped")
            return
        }
        val hash = sha256(text.encodeToByteArray()).joinToString("") { it.toString(16).padStart(2, '0') }
        // Same content already captured — dedup and loop suppression in one step.
        if (ClipboardHelper.getLatestByHash(hash) != null) return
        val sensitive = Build.VERSION.SDK_INT >= 33 &&
            clip.description.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) == true
        val entry = DClipboard(
            id = generateId(),
            text = text,
            hash = hash,
            sensitive = sensitive,
            createdAt = TimeHelper.now(),
        )
        ClipboardHelper.insert(entry)
        if (sensitive) return
        sendEvent(
            WebSocketEvent(
                EventType.CLIPBOARD_CHANGED,
                JsonHelper.jsonEncode(
                    ClipboardChangedData(
                        text = text,
                        sensitive = false,
                        time = entry.createdAt.toEpochMilliseconds(),
                    )
                ),
            )
        )
    }
}
