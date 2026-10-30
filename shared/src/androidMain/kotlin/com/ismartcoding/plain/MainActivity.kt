package com.ismartcoding.plain

import com.ismartcoding.plain.ui.app.installComposeUi
import com.ismartcoding.plain.ui.app.pendingCrashReport

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.CursorWindow
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.plain.platform.isTPlus
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.features.files.ExportFileType
import com.ismartcoding.plain.features.settings.Language
import com.ismartcoding.plain.features.files.PickFileTag
import com.ismartcoding.plain.features.files.PickFileType
import com.ismartcoding.plain.events.ExportFileResultEvent
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.ui.permissions.Permissions
import com.ismartcoding.plain.features.bluetooth.client.BluetoothPermission
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.platform.system.publishLauncherShortcuts
import com.ismartcoding.plain.receivers.NetworkStateReceiver
import com.ismartcoding.plain.services.PlainAccessibilityService
import com.ismartcoding.plain.services.ScreenMirrorService
import com.ismartcoding.plain.platform.system.FilePickHelper
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.chat.ChannelViewModel
import com.ismartcoding.plain.ui.chat.PeerViewModel
import com.ismartcoding.plain.ui.pomodoro.PomodoroViewModel
import com.ismartcoding.plain.lib.sendEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import android.Manifest
import com.ismartcoding.plain.platform.system.BackgroundExecution
import com.ismartcoding.plain.ui.app.ActivityUiState

class MainActivity : AppCompatActivity() {
    internal var pickFileType = PickFileType.IMAGE
    internal var pickFileTag = PickFileTag.SEND_MESSAGE
    internal var exportFileType = ExportFileType.OPML
    internal val audioQueueVM: AudioQueueViewModel by viewModels()
    val pomodoroVM: PomodoroViewModel by viewModels()
    internal val peerVM: PeerViewModel by viewModels()
    internal val channelVM: ChannelViewModel by viewModels()
    internal val uiState = ActivityUiState()

    internal val screenCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null && ScreenMirrorService.instance == null) {
            ContextCompat.startForegroundService(
                this, Intent(this, ScreenMirrorService::class.java)
                    .putExtra("code", result.resultCode).putExtra("data", result.data)
            )
        }
    }
    internal val recordAudioForMirror = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        try {
            screenCapture.launch(mediaProjectionManager.createScreenCaptureIntent())
        } catch (e: IllegalStateException) {
            LogCat.e("Error launching screen capture: ${e.message}")
        }
    }
    internal val recordAudioForMirrorLate = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            sendScreenMirrorAudioStatus(true)
        } else if (!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
            && !Permission.RECORD_AUDIO.isGranted()
        ) {
            showRecordAudioPermissionSettingsGuide()
        } else {
            sendScreenMirrorAudioStatus(false)
        }
    }
    internal val appDetailsSettingsForAudioLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        sendScreenMirrorAudioStatus(Permission.RECORD_AUDIO.isGranted())
    }
    internal val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) sendEvent(PickFileResultEvent(pickFileTag, pickFileType, setOf(uri.toString())))
    }
    internal val pickMultipleMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            if (uris.isNotEmpty()) {
                sendEvent(PickFileResultEvent(pickFileTag, pickFileType, uris.map { it.toString() }.toSet()))
            }
        }
    internal val pickFileActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uris = result.data?.let { FilePickHelper.getUris(it) } ?: emptySet()
        if (uris.isNotEmpty()) {
            if (pickFileType == PickFileType.FOLDER) {
                val data = result.data
                val resultFlags = data?.flags ?: 0
                val grantFlags = resultFlags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                if (data != null && resultFlags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0 && grantFlags != 0) {
                    uris.forEach { uri ->
                        runCatching { contentResolver.takePersistableUriPermission(uri, grantFlags) }
                            .onFailure { LogCat.e("Unable to persist selected folder permission for $uri", it) }
                    }
                }
            }
            sendEvent(PickFileResultEvent(pickFileTag, pickFileType, uris.map { it.toString() }.toSet()))
        }
    }
    internal val exportFileActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (uri != null) {
            sendEvent(ExportFileResultEvent(exportFileType, uri.toString()))
        }
    }
    private val networkStateReceiver = NetworkStateReceiver()

    override fun onStart() {
        super.onStart()
        BackgroundExecution.executionStateChanged()
    }

    override fun onStop() {
        super.onStop()
        BackgroundExecution.executionStateChanged()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        sendEvent(WindowFocusChangedEvent(hasFocus))
        BackgroundExecution.executionStateChanged()
    }

    @SuppressLint("ClickableViewAccessibility", "DiscouragedPrivateApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen(); super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        Language.applyLocale()
        WindowCompat.getInsetsController(window, window.decorView).systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        instance = WeakReference(this)
        mainActivity = this
        pendingCrashReport = CrashHandler.getPendingReport()
        try {
            val f = CursorWindow::class.java.getDeclaredField("sCursorWindowSize"); f.isAccessible = true; f.set(null, 100 * 1024 * 1024)
        } catch (_: Exception) {
        }
        BluetoothPermission.init(this); Permissions.init(this); initEvents()
        if (isTPlus()) {
            registerReceiver(networkStateReceiver, IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(networkStateReceiver, IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION))
        }
        installComposeUi()
        lifecycleScope.launch(Dispatchers.Default) {
            publishLauncherShortcuts(Prefs.selectedLauncherShortcuts(Prefs.launcherShortcuts.value))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Permissions.release()
        unregisterReceiver(networkStateReceiver)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        PlainAccessibilityService.invalidateScreenSizeCache()
        Language.applyLocale()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); handleIntent(intent)
    }

    fun openNew() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            }
            startActivity(intent)
        } catch (e: Exception) {
            LogCat.e("Error bringing MainActivity to foreground: ${e.message}")
        }
    }

    companion object {
        lateinit var instance: WeakReference<MainActivity>
    }
}
