package com.ismartcoding.plain.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.CursorWindow
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.isTPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.Language
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.ExportFileResultEvent
import com.ismartcoding.plain.events.IgnoreBatteryOptimizationResultEvent
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.bluetooth.BluetoothPermission
import com.ismartcoding.plain.preferences.SettingsProvider
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.receivers.NetworkStateReceiver
import com.ismartcoding.plain.receivers.PlugInControlReceiver
import com.ismartcoding.plain.services.PlainAccessibilityService
import com.ismartcoding.plain.services.ScreenMirrorService
import com.ismartcoding.plain.ui.helpers.FilePickHelper
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.PomodoroViewModel
import com.ismartcoding.plain.CrashHandler
import com.ismartcoding.plain.ui.page.CrashReportDialog
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.Main
import com.ismartcoding.plain.ui.page.chat.components.ForwardTarget
import com.ismartcoding.plain.ui.page.chat.components.ForwardTargetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    internal var pickFileType = PickFileType.IMAGE
    internal var pickFileTag = PickFileTag.SEND_MESSAGE
    internal var exportFileType = ExportFileType.OPML
    internal var requestToConnectDialog: AlertDialog? = null
    internal var pairingRequestDialog: AlertDialog? = null
    internal var channelInviteDialog: AlertDialog? = null
    internal val mainVM: MainViewModel by viewModels()
    internal val audioPlaylistVM: AudioPlaylistViewModel by viewModels()
    val pomodoroVM: PomodoroViewModel by viewModels()
    internal val peerVM: PeerViewModel by viewModels()
    internal val channelVM: ChannelViewModel by viewModels()
    internal val chatVM: ChatViewModel by viewModels()
    internal val navControllerState = mutableStateOf<NavHostController?>(null)
    internal var showForwardTargetDialog by mutableStateOf(false)
    internal var pendingFileUris by mutableStateOf<Set<Uri>?>(null)
    internal var pendingCrashReport by mutableStateOf<String?>(null)

    internal val screenCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null && ScreenMirrorService.instance == null) {
            ContextCompat.startForegroundService(this, Intent(this, ScreenMirrorService::class.java)
                .putExtra("code", result.resultCode).putExtra("data", result.data))
        }
    }
    internal val recordAudioForMirror = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        try { screenCapture.launch(com.ismartcoding.plain.mediaProjectionManager.createScreenCaptureIntent()) }
        catch (e: IllegalStateException) { LogCat.e("Error launching screen capture: ${e.message}") }
    }
    internal val recordAudioForMirrorLate = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) sendScreenMirrorAudioStatus(true)
        else if (!shouldShowRequestPermissionRationale(android.Manifest.permission.RECORD_AUDIO) && !Permission.RECORD_AUDIO.can(this)) showRecordAudioPermissionSettingsGuide()
        else sendScreenMirrorAudioStatus(false)
    }
    internal val appDetailsSettingsForAudioLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { sendScreenMirrorAudioStatus(Permission.RECORD_AUDIO.can(this)) }
    internal val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) sendEvent(PickFileResultEvent(pickFileTag, pickFileType, setOf(uri))) }
    internal val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris -> if (uris.isNotEmpty()) sendEvent(PickFileResultEvent(pickFileTag, pickFileType, uris.toSet())) }
    internal val pickFileActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uris = result.data?.let { FilePickHelper.getUris(it) } ?: emptySet()
        if (uris.isNotEmpty()) sendEvent(PickFileResultEvent(pickFileTag, pickFileType, uris))
    }
    internal val exportFileActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (uri != null) sendEvent(ExportFileResultEvent(exportFileType, uri))
    }
    internal val ignoreBatteryOptimizationActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { sendEvent(IgnoreBatteryOptimizationResultEvent()) }

    private val plugInReceiver = PlugInControlReceiver()
    private val networkStateReceiver = NetworkStateReceiver()

    override fun onWindowFocusChanged(hasFocus: Boolean) { sendEvent(WindowFocusChangedEvent(hasFocus)) }

    @SuppressLint("ClickableViewAccessibility", "DiscouragedPrivateApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen(); super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false); enableEdgeToEdge()
        lifecycleScope.launch(Dispatchers.IO) { Language.initLocaleAsync(this@MainActivity) }
        WindowCompat.getInsetsController(window, window.decorView).systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        instance = WeakReference(this)
        pendingCrashReport = CrashHandler.getPendingReport(this)
        try { val f = CursorWindow::class.java.getDeclaredField("sCursorWindowSize"); f.isAccessible = true; f.set(null, 100 * 1024 * 1024) } catch (_: Exception) {}
        BluetoothPermission.init(this); Permissions.init(this); initEvents()
        val powerFilter = IntentFilter().apply { addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED) }
        if (isTPlus()) { registerReceiver(plugInReceiver, powerFilter, RECEIVER_NOT_EXPORTED); registerReceiver(networkStateReceiver, IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION), RECEIVER_NOT_EXPORTED) }
        else { registerReceiver(plugInReceiver, powerFilter); registerReceiver(networkStateReceiver, IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)) }
        setContent {
            SettingsProvider {
                Main(navControllerState, onLaunched = { handleIntent(intent) }, mainVM, audioPlaylistVM, pomodoroVM, chatVM = chatVM, peerVM = peerVM, channelVM = channelVM)
                if (showForwardTargetDialog) {
                    ForwardTargetDialog(peerVM = peerVM, onDismiss = { showForwardTargetDialog = false; pendingFileUris = null },
                        onTargetSelected = { target -> pendingFileUris?.let { uris ->
                            val route = when (target) { is ForwardTarget.Local -> Routing.Chat("local"); is ForwardTarget.Peer -> Routing.Chat("peer:${target.peer.id}") }
                            navControllerState.value?.navigate(route); coIO { delay(1000); sendEvent(PickFileResultEvent(PickFileTag.SEND_MESSAGE, PickFileType.FILE, uris)) }
                        } })
                }
                pendingCrashReport?.let { report ->
                    CrashReportDialog(crashReport = report, onDismiss = { pendingCrashReport = null })
                }
            }
        }
        AudioPlayer.ensurePlayer(this)
        coIO { try { if (WebPreference.getAsync(this@MainActivity)) mainVM.enableHttpServer(this@MainActivity, true); doWhenReadyAsync() } catch (ex: Exception) { LogCat.e(ex.toString()) } }
    }

    override fun onDestroy() { super.onDestroy(); Permissions.release(); unregisterReceiver(plugInReceiver); unregisterReceiver(networkStateReceiver) }
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); PlainAccessibilityService.invalidateScreenSizeCache(); lifecycleScope.launch(Dispatchers.IO) { Language.initLocaleAsync(this@MainActivity) } }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleIntent(intent) }

    companion object { lateinit var instance: WeakReference<MainActivity> }
}
