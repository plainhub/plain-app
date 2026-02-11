package com.ismartcoding.plain.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.CursorWindow
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.capitalize
import com.ismartcoding.lib.extensions.getSystemScreenTimeout
import com.ismartcoding.lib.extensions.parcelable
import com.ismartcoding.lib.extensions.parcelableArrayList
import com.ismartcoding.lib.extensions.setSystemScreenTimeout
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.isTPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.enums.Language
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.ExportFileEvent
import com.ismartcoding.plain.events.ExportFileResultEvent
import com.ismartcoding.plain.events.HttpServerStateChangedEvent
import com.ismartcoding.plain.events.IgnoreBatteryOptimizationEvent
import com.ismartcoding.plain.events.IgnoreBatteryOptimizationResultEvent
import com.ismartcoding.plain.events.PairingCancelledEvent
import com.ismartcoding.plain.events.PairingRequestReceivedEvent
import com.ismartcoding.plain.events.PairingResponseEvent
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.events.RestartAppEvent
import com.ismartcoding.plain.events.StartHttpServerEvent
import com.ismartcoding.plain.events.StartScreenMirrorEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.features.ChatHelper
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.bluetooth.BluetoothPermission
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.locale.LocaleHelper.getStringF
import com.ismartcoding.plain.helpers.ScreenHelper
import com.ismartcoding.plain.mediaProjectionManager
import com.ismartcoding.plain.preferences.ApiPermissionsPreference
import com.ismartcoding.plain.preferences.KeepScreenOnPreference
import com.ismartcoding.plain.preferences.SettingsProvider
import com.ismartcoding.plain.preferences.SystemScreenTimeoutPreference
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.receivers.NetworkStateReceiver
import com.ismartcoding.plain.receivers.PlugInControlReceiver
import com.ismartcoding.plain.services.PNotificationListenerService
import com.ismartcoding.plain.services.ScreenMirrorService
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.FilePickHelper
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChatListViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.PomodoroViewModel
import com.ismartcoding.plain.ui.page.chat.components.ForwardTarget
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.nav.navigatePdf
import com.ismartcoding.plain.ui.nav.navigateTextFile
import com.ismartcoding.plain.ui.page.Main
import com.ismartcoding.plain.ui.page.chat.components.ForwardTargetDialog
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.web.models.toModel
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private var pickFileType = PickFileType.IMAGE
    private var pickFileTag = PickFileTag.SEND_MESSAGE
    private var exportFileType = ExportFileType.OPML
    private var requestToConnectDialog: AlertDialog? = null
    private var pairingRequestDialog: AlertDialog? = null
    private val mainVM: MainViewModel by viewModels()
    private val audioPlaylistVM: AudioPlaylistViewModel by viewModels()
    val pomodoroVM: PomodoroViewModel by viewModels()
    private val chatListVM: ChatListViewModel by viewModels()
    private val navControllerState = mutableStateOf<NavHostController?>(null)
    
    private var showForwardTargetDialog by mutableStateOf(false)
    private var pendingFileUris by mutableStateOf<Set<Uri>?>(null)

    private val screenCapture =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                if (ScreenMirrorService.instance == null) {
                    val service = Intent(this, ScreenMirrorService::class.java)
                    service.putExtra("code", result.resultCode)
                    service.putExtra("data", result.data)
                    ContextCompat.startForegroundService(this, service)
                }
            }
        }

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                sendEvent(PickFileResultEvent(pickFileTag, pickFileType, setOf(uri)))
            }
        }

    private val pickMultipleMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            if (uris.isNotEmpty()) {
                sendEvent(PickFileResultEvent(pickFileTag, pickFileType, uris.toSet()))
            }
        }

    private val pickFileActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                sendEvent(PickFileResultEvent(pickFileTag, pickFileType, FilePickHelper.getUris(result.data!!)))
            }
        }

    private val exportFileActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data?.data != null) {
                    sendEvent(ExportFileResultEvent(exportFileType, data.data!!))
                }
            }
        }

    private val ignoreBatteryOptimizationActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            sendEvent(IgnoreBatteryOptimizationResultEvent())
        }

    private fun fixSystemBarsAnimation() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        LogCat.d("onWindowFocusChanged: $hasFocus")
        sendEvent(WindowFocusChangedEvent(hasFocus))
    }

    private val plugInReceiver = PlugInControlReceiver()
    private val networkStateReceiver = NetworkStateReceiver()

    @SuppressLint("ClickableViewAccessibility", "DiscouragedPrivateApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        lifecycleScope.launch(Dispatchers.IO) {
            Language.initLocaleAsync(this@MainActivity)
        }
        fixSystemBarsAnimation()

        instance = WeakReference(this)
        // https://stackoverflow.com/questions/51959944/sqliteblobtoobigexception-row-too-big-to-fit-into-cursorwindow-requiredpos-0-t
        try {
            val field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
            field.isAccessible = true
            field.set(null, 100 * 1024 * 1024) // the 100MB is the new size
        } catch (e: Exception) {
            e.printStackTrace()
        }

        BluetoothPermission.init(this)
        Permissions.init(this)
        initEvents()
        val powerConnectionFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        if (isTPlus()) {
            registerReceiver(plugInReceiver, powerConnectionFilter, RECEIVER_NOT_EXPORTED)
            registerReceiver(networkStateReceiver, IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(plugInReceiver, powerConnectionFilter)
            registerReceiver(networkStateReceiver, IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION))
        }

        setContent {
            SettingsProvider {
                Main(navControllerState, onLaunched = {
                    handleIntent(intent)
                }, mainVM, audioPlaylistVM, pomodoroVM)
                
                if (showForwardTargetDialog) {
                    ForwardTargetDialog(
                        chatListVM = chatListVM,
                        onDismiss = {
                            showForwardTargetDialog = false
                            pendingFileUris = null
                        },
                        onTargetSelected = { target ->
                            pendingFileUris?.let { uris ->
                                when (target) {
                                    is ForwardTarget.Local -> {
                                        navControllerState.value?.navigate(Routing.Chat("local"))
                                        coIO {
                                            delay(1000)
                                            sendEvent(PickFileResultEvent(PickFileTag.SEND_MESSAGE, PickFileType.FILE, uris))
                                        }
                                    }
                                    is ForwardTarget.Peer -> {
                                        navControllerState.value?.navigate(Routing.Chat("peer:${target.peer.id}"))
                                        coIO {
                                            delay(1000)
                                            sendEvent(PickFileResultEvent(PickFileTag.SEND_MESSAGE, PickFileType.FILE, uris))
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        AudioPlayer.ensurePlayer(this@MainActivity)
        coIO {
            try {
                val webEnabled = WebPreference.getAsync(this@MainActivity)
                if (webEnabled) {
                    mainVM.enableHttpServer(this@MainActivity, true)
                }
                doWhenReadyAsync()
            } catch (ex: Exception) {
                LogCat.e(ex.toString())
            }
        }
    }

    private suspend fun doWhenReadyAsync() {
        // PackageHelper.cacheAppLabels()
        val webEnabled = WebPreference.getAsync(this@MainActivity)
        val permissionEnabled = Permission.NOTIFICATION_LISTENER.isEnabledAsync(this@MainActivity)
        PNotificationListenerService.toggle(this@MainActivity, webEnabled && permissionEnabled)
    }

    override fun onDestroy() {
        super.onDestroy()
        Permissions.release()
        unregisterReceiver(plugInReceiver)
        unregisterReceiver(networkStateReceiver)
    }

    @SuppressLint("CheckResult")
    private fun initEvents() {
        lifecycleScope.launch {
            Channel.sharedFlow.collect { event ->
                // Check if activity is still valid before processing events
                if (isDestroyed || isFinishing) {
                    return@collect
                }

                when (event) {
                    is HttpServerStateChangedEvent -> {
                        mainVM.httpServerError = HttpServerManager.httpServerError
                        mainVM.httpServerState = event.state
                        if (event.state == HttpServerState.ON && !Permission.WRITE_EXTERNAL_STORAGE.can(this@MainActivity)) {
                            DialogHelper.showConfirmDialog(
                                LocaleHelper.getString(R.string.confirm),
                                LocaleHelper.getString(R.string.storage_permission_confirm)
                            ) {
                                coIO {
                                    ApiPermissionsPreference.putAsync(this@MainActivity, Permission.WRITE_EXTERNAL_STORAGE, true)
                                    sendEvent(RequestPermissionsEvent(Permission.WRITE_EXTERNAL_STORAGE))
                                }
                            }
                        }
                    }

                    is PermissionsResultEvent -> {
                        if (event.map.containsKey(Permission.WRITE_SETTINGS.toSysPermission()) && Permission.WRITE_SETTINGS.can(this@MainActivity)) {
                            val enable = !KeepScreenOnPreference.getAsync(this@MainActivity)
                            ScreenHelper.saveOn(this@MainActivity, enable)
                            if (enable) {
                                ScreenHelper.saveTimeout(this@MainActivity, contentResolver.getSystemScreenTimeout())
                                contentResolver.setSystemScreenTimeout(Int.MAX_VALUE)
                            } else {
                                val systemScreenTimeout = SystemScreenTimeoutPreference.getAsync(this@MainActivity)
                                contentResolver.setSystemScreenTimeout(
                                    if (systemScreenTimeout > 0) systemScreenTimeout else 5000 * 60,
                                ) // default 5 minutes
                            }
                        }
                    }

                    is StartScreenMirrorEvent -> {
                        try {
                            screenCapture.launch(mediaProjectionManager.createScreenCaptureIntent())
                        } catch (e: IllegalStateException) {
                            LogCat.e("Error launching screen capture: ${e.message}")
                        }
                    }

                    is IgnoreBatteryOptimizationEvent -> {
                        try {
                            val intent = Intent()
                            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            intent.data = Uri.parse("package:$packageName")
                            ignoreBatteryOptimizationActivityLauncher.launch(intent)
                        } catch (e: IllegalStateException) {
                            LogCat.e("Error launching battery optimization: ${e.message}")
                        }
                    }

                    is RestartAppEvent -> {
                        val intent = Intent(this@MainActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        Runtime.getRuntime().exit(0)
                    }

                    is PickFileEvent -> {
                        try {
                            pickFileType = event.type
                            pickFileTag = event.tag
                            var type: ActivityResultContracts.PickVisualMedia.VisualMediaType? = null
                            when (event.type) {
                                PickFileType.IMAGE_VIDEO -> {
                                    type = ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                }

                                PickFileType.IMAGE -> {
                                    type = ActivityResultContracts.PickVisualMedia.ImageOnly
                                }

                                else -> {}
                            }
                            if (type != null) {
                                try {
                                    if (event.multiple) {
                                        pickMultipleMedia.launch(PickVisualMediaRequest(type))
                                    } else {
                                        pickMedia.launch(PickVisualMediaRequest(type))
                                    }
                                } catch (e: ActivityNotFoundException) {
                                    LogCat.e("Photo picker not available, falling back to file picker")
                                    doPickFile(event)
                                }
                            } else {
                                doPickFile(event)
                            }
                        } catch (e: IllegalStateException) {
                            LogCat.e("Error launching pick file: ${e.message}")
                        }
                    }

                    is ExportFileEvent -> {
                        try {
                            exportFileType = event.type
                            exportFileActivityLauncher.launch(
                                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    type = "text/*"
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    putExtra(Intent.EXTRA_TITLE, event.fileName)
                                },
                            )
                        } catch (e: ActivityNotFoundException) {
                            LogCat.e("No document creation app available")
                            DialogHelper.showMessage(getString(R.string.file_picker_not_available))
                        } catch (e: IllegalStateException) {
                            LogCat.e("Error launching export file: ${e.message}")
                        }
                    }

                    is ConfirmToAcceptLoginEvent -> {
                        try {
                            val clientIp = HttpServerManager.clientIpCache[event.clientId] ?: ""
                            if (requestToConnectDialog?.isShowing == true) {
                                requestToConnectDialog?.dismiss()
                                requestToConnectDialog = null
                            }

                            val r = event.request
                            requestToConnectDialog =
                                AlertDialog.Builder(instance.get()!!)
                                    .setTitle(getStringF(R.string.request_to_connect, "ip", clientIp))
                                    .setMessage(
                                        getStringF(
                                            R.string.client_ua,
                                            "os_name",
                                            r.osName.capitalize(),
                                            "os_version",
                                            r.osVersion,
                                            "browser_name",
                                            r.browserName.capitalize(),
                                            "browser_version",
                                            r.browserVersion,
                                        ),
                                    )
                                    .setPositiveButton(getString(R.string.accept)) { _, _ ->
                                        launch(Dispatchers.IO) {
                                            HttpServerManager.respondTokenAsync(event, clientIp)
                                        }
                                    }
                                    .setNegativeButton(getString(R.string.reject)) { _, _ ->
                                        launch(Dispatchers.IO) {
                                            event.session.close(
                                                CloseReason(
                                                    CloseReason.Codes.TRY_AGAIN_LATER, "rejected",
                                                ),
                                            )
                                        }
                                    }.create()

                            if (Permission.SYSTEM_ALERT_WINDOW.can(this@MainActivity)) {
                                requestToConnectDialog?.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                            }
                            requestToConnectDialog?.window?.setDimAmount(0.8f)
                            requestToConnectDialog?.show()
                        } catch (e: Exception) {
                            LogCat.e("Error showing connect dialog: ${e.message}")
                            requestToConnectDialog = null
                        }
                    }

                    is PairingRequestReceivedEvent -> {
                        try {
                            if (pairingRequestDialog?.isShowing == true) {
                                pairingRequestDialog?.dismiss()
                                pairingRequestDialog = null
                            }

                            val request = event.request
                            pairingRequestDialog =
                                AlertDialog.Builder(instance.get()!!)
                                    .setTitle(getString(R.string.pairing_request))
                                    .setMessage(getString(R.string.pairing_request_message, request.fromName))
                                    .setPositiveButton(getString(R.string.allow)) { _, _ ->
                                        sendEvent(PairingResponseEvent(request, event.fromIp, true))
                                    }
                                    .setNegativeButton(getString(R.string.deny)) { _, _ ->
                                        sendEvent(PairingResponseEvent(request, event.fromIp, false))
                                    }
                                    .setCancelable(false)
                                    .create()

                            if (Permission.SYSTEM_ALERT_WINDOW.can(this@MainActivity)) {
                                pairingRequestDialog?.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                            }
                            pairingRequestDialog?.window?.setDimAmount(0.8f)
                            pairingRequestDialog?.show()
                        } catch (e: Exception) {
                            LogCat.e("Error showing pairing dialog: ${e.message}")
                            pairingRequestDialog = null
                        }
                    }
                    
                    is PairingCancelledEvent -> {
                        try {
                            if (pairingRequestDialog?.isShowing == true) {
                                pairingRequestDialog?.dismiss()
                                pairingRequestDialog = null
                                LogCat.d("Pairing request dialog closed due to cancellation from remote device")
                            }
                        } catch (e: Exception) {
                            LogCat.e("Error closing pairing dialog: ${e.message}")
                            pairingRequestDialog = null
                        }
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        lifecycleScope.launch(Dispatchers.IO) {
            Language.initLocaleAsync(this@MainActivity)
        }
    }

    private fun doPickFile(event: PickFileEvent) {
        try {
            val intent = when (event.type) {
                PickFileType.FOLDER -> FilePickHelper.getPickFolderIntent()
                else -> FilePickHelper.getPickFileIntent(event.multiple)
            }
            pickFileActivityLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            LogCat.w("ACTION_OPEN_DOCUMENT not supported, trying fallback")
            try {
                // Only try fallback for file selection, not folder selection
                if (event.type != PickFileType.FOLDER) {
                    pickFileActivityLauncher.launch(FilePickHelper.getFallbackPickFileIntent(event.multiple))
                } else {
                    LogCat.e("No folder picker available on this device")
                    DialogHelper.showErrorMessage(getString(R.string.file_picker_not_available))
                }
            } catch (e2: ActivityNotFoundException) {
                LogCat.e("No file picker available on this device")
                DialogHelper.showErrorMessage(getString(R.string.file_picker_not_available))
            } catch (e2: IllegalStateException) {
                LogCat.e("Error launching fallback pick file activity: ${e2.message}")
            }
        } catch (e: IllegalStateException) {
            LogCat.e("Error launching pick file activity: ${e.message}")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.getBooleanExtra("start_web_service", false)) {
            coIO {
                WebPreference.putAsync(this@MainActivity, true)
                sendEvent(StartHttpServerEvent())
            }
        }
        
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                val mimeType = contentResolver.getType(uri)
                if (mimeType != null) {
                    if (mimeType.startsWith("text/")) {
                        navControllerState.value?.navigateTextFile(uri.toString())
                    } else if (mimeType == "application/pdf") {
                        navControllerState.value?.navigatePdf(uri)
                    } else {
                        DialogHelper.showErrorMessage(LocaleHelper.getString(R.string.not_supported_error))
                    }
                } else {
                    DialogHelper.showErrorMessage(LocaleHelper.getString(R.string.not_supported_error))
                }
            }
        } else if (intent.action == Intent.ACTION_SEND) {
            if (intent.type?.startsWith("text/") == true) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                coMain {
                    val item = withIO {
                        ChatHelper.sendAsync(DMessageContent(DMessageType.TEXT.value, DMessageText(sharedText)))
                    }
                    val m = item.toModel()
                    m.data = m.getContentData()
                    sendEvent(
                        WebSocketEvent(
                            EventType.MESSAGE_CREATED,
                            JsonHelper.jsonEncode(
                                arrayListOf(
                                    m
                                ),
                            ),
                        ),
                    )
                    navControllerState.value?.navigate(Routing.Chat("local"))
                }
                return
            }

            val uri = intent.parcelable(Intent.EXTRA_STREAM) as? Uri ?: return
            coMain {
                DialogHelper.showLoading()
                withIO { chatListVM.loadPeers() }
                DialogHelper.hideLoading()
                pendingFileUris = setOf(uri)
                showForwardTargetDialog = true
            }
        } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val uris = intent.parcelableArrayList<Uri>(Intent.EXTRA_STREAM)
            if (uris != null) {
                coMain {
                    DialogHelper.showLoading()
                    withIO { chatListVM.loadPeers() }
                    DialogHelper.hideLoading()
                    pendingFileUris = uris.toSet()
                    showForwardTargetDialog = true
                }
            }
        }
    }

    companion object {
        lateinit var instance: WeakReference<MainActivity>
    }
}
