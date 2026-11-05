package com.ismartcoding.plain.features


import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.features.web.AccessFeature
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.events.ClipboardReportData
import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object ClipboardWatcher {
    private const val MAX_TEXT_LENGTH = 256 * 1024

    @Volatile private var scope: CoroutineScope? = null
    private var report: (suspend (ClipboardReportData) -> Unit)? = null
    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var eventJob: Job? = null
    private var logcatJob: Job? = null
    private var logcatProcess: Process? = null

    fun canSyncAutomatically(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_LOGS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Synchronized
    fun start(scope: CoroutineScope, report: suspend (ClipboardReportData) -> Unit) {
        check(this.scope == null) { "Clipboard observation already started" }
        this.scope = scope
        this.report = report
        try {
            val l = ClipboardManager.OnPrimaryClipChangedListener { handleChange() }
            clipboardManager.addPrimaryClipChangedListener(l)
            listener = l
        } catch (e: Exception) {
            LogCat.e("ClipboardWatcher start failed: ${e.message}")
        }
        eventJob = scope.launch(Dispatchers.IO) {
            if ((AccessFeature.CLIPBOARD in Prefs.apiAccessFeatures.value)) startLogcat()
            Channel.sharedFlow.collect { event ->
                when (event) {
                    is WindowFocusChangedEvent -> if (event.hasFocus) handleFocusGained()
                    else -> Unit
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        scope = null
        report = null
        listener?.let {
            runCatching { clipboardManager.removePrimaryClipChangedListener(it) }
                .onFailure { LogCat.e("ClipboardWatcher stop failed: ${it.message}") }
        }
        listener = null
        stopLogcat()
        eventJob?.cancel()
        eventJob = null
    }

    fun handleFocusGained() {
        scope?.launch(Dispatchers.IO) {
            try {
                handleClipboardChange()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                LogCat.e("ClipboardWatcher focus read failed: ${e.message}")
            }
        }
    }

    @Synchronized
    private fun startLogcat() {
        val scope = scope ?: return
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
            logcatJob = scope.launch(Dispatchers.IO) {
                BufferedReader(InputStreamReader(process.inputStream)).forEachLine { line ->
                    if (isActive && line.contains(appContext.packageName)) {
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
        scope?.launch(Dispatchers.IO) {
            try {
                handleClipboardChange()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                LogCat.e("ClipboardWatcher handle failed: ${e.message}")
            }
        }
    }

    private suspend fun handleClipboardChange() {
        if (AccessFeature.CLIPBOARD !in Prefs.apiAccessFeatures.value) return
        val clip = clipboardManager.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(appContext)?.toString() ?: return
        if (text.isBlank()) return
        if (text.length > MAX_TEXT_LENGTH) {
            LogCat.d("ClipboardWatcher: clip too large (${text.length} chars), skipped")
            return
        }
        val sensitive = Build.VERSION.SDK_INT >= 33 &&
            clip.description.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) == true
        report?.invoke(ClipboardReportData(text, sensitive))
    }
}
