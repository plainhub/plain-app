package com.ismartcoding.plain.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.CursorWindow
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.capitalize
import com.ismartcoding.lib.extensions.dp2px
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
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.enums.AppChannelType
import com.ismartcoding.plain.enums.AudioAction
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.enums.Language
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.features.AudioActionEvent
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.features.ChatHelper
import com.ismartcoding.plain.features.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.features.ExportFileEvent
import com.ismartcoding.plain.features.ExportFileResultEvent
import com.ismartcoding.plain.features.HttpServerStateChangedEvent
import com.ismartcoding.plain.features.IgnoreBatteryOptimizationEvent
import com.ismartcoding.plain.features.IgnoreBatteryOptimizationResultEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.PermissionsResultEvent
import com.ismartcoding.plain.features.PickFileEvent
import com.ismartcoding.plain.features.PickFileResultEvent
import com.ismartcoding.plain.features.RequestPermissionsEvent
import com.ismartcoding.plain.features.RestartAppEvent
import com.ismartcoding.plain.features.StartScreenMirrorEvent
import com.ismartcoding.plain.features.WindowFocusChangedEvent
import com.ismartcoding.plain.features.bluetooth.BluetoothPermission
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.locale.LocaleHelper.getStringF
import com.ismartcoding.plain.helpers.ScreenHelper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.mediaProjectionManager
import com.ismartcoding.plain.preference.AgreeTermsPreference
import com.ismartcoding.plain.preference.ApiPermissionsPreference
import com.ismartcoding.plain.preference.KeepScreenOnPreference
import com.ismartcoding.plain.preference.SettingsProvider
import com.ismartcoding.plain.preference.SystemScreenTimeoutPreference
import com.ismartcoding.plain.preference.WebPreference
import com.ismartcoding.plain.receivers.NetworkStateReceiver
import com.ismartcoding.plain.receivers.PlugInControlReceiver
import com.ismartcoding.plain.services.PNotificationListenerService
import com.ismartcoding.plain.services.ScreenMirrorService
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.FilePickHelper
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.nav.navigatePdf
import com.ismartcoding.plain.ui.nav.navigateTextFile
import com.ismartcoding.plain.ui.page.Main
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.web.models.toModel
import com.ismartcoding.plain.web.websocket.EventType
import com.ismartcoding.plain.web.websocket.WebSocketEvent
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
    private val mainVM: MainViewModel by viewModels()
    private val audioPlaylistVM: AudioPlaylistViewModel by viewModels()
    private val navControllerState = mutableStateOf<NavHostController?>(null)

    private val screenCapture =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val image = ScreenMirrorService.instance?.getLatestImage()
                if (image == null) {
                    val service = Intent(this, ScreenMirrorService::class.java)
                    service.putExtra("code", result.resultCode)
                    service.putExtra("data", result.data)
                    startService(service)
                } else {
                    sendEvent(WebSocketEvent(EventType.SCREEN_MIRRORING, image))
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
                }, mainVM, audioPlaylistVM)
            }
        }

        AudioPlayer.ensurePlayer(this@MainActivity)
        coIO {
            try {
                if (BuildConfig.CHANNEL == AppChannelType.CHINA.name && !AgreeTermsPreference.getAsync(this@MainActivity)) {
                    coMain {
                        showTermsAndPrivacyDialog(this@MainActivity)
                    }
                } else {
                    val webEnabled = WebPreference.getAsync(this@MainActivity)
                    if (webEnabled) {
                        mainVM.enableHttpServer(this@MainActivity, true)
                    }
                    doWhenReadyAsync()
                }
            } catch (ex: Exception) {
                LogCat.e(ex.toString())
            }
        }

    }

    private suspend fun doWhenReadyAsync() {
        // PackageHelper.cacheAppLabels()
        PNotificationListenerService.toggle(this@MainActivity, Permission.NOTIFICATION_LISTENER.isEnabledAsync(this@MainActivity))
    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothPermission.release()
        Permissions.release()
        unregisterReceiver(plugInReceiver)
        unregisterReceiver(networkStateReceiver)
    }

    @SuppressLint("CheckResult")
    private fun initEvents() {
        receiveEvent<AudioActionEvent> {
            when (it.action) {
                AudioAction.MEDIA_ITEM_TRANSITION -> {
                    coIO {
                        audioPlaylistVM.loadAsync(this@MainActivity)
                    }
                }

                else -> {
                }
            }
        }
        receiveEvent<HttpServerStateChangedEvent> {
            mainVM.httpServerError = HttpServerManager.httpServerError
            mainVM.httpServerState = it.state
            if (it.state == HttpServerState.ON && !Permission.WRITE_EXTERNAL_STORAGE.can(this@MainActivity)) {
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

        receiveEvent<PermissionsResultEvent> { event ->
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

        receiveEvent<StartScreenMirrorEvent> {
            screenCapture.launch(mediaProjectionManager.createScreenCaptureIntent())
        }

        receiveEvent<IgnoreBatteryOptimizationEvent> {
            val intent = Intent()
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:$packageName")
            ignoreBatteryOptimizationActivityLauncher.launch(intent)
        }

        receiveEvent<RestartAppEvent> {
            val intent = Intent(this@MainActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            Runtime.getRuntime().exit(0)
        }

        receiveEvent<PickFileEvent> {
            pickFileType = it.type
            pickFileTag = it.tag
            var type: ActivityResultContracts.PickVisualMedia.VisualMediaType? = null
            when (it.type) {
                PickFileType.IMAGE_VIDEO -> {
                    type = ActivityResultContracts.PickVisualMedia.ImageAndVideo
                }

                PickFileType.IMAGE -> {
                    type = ActivityResultContracts.PickVisualMedia.ImageOnly
                }

                else -> {}
            }
            if (type != null) {
                if (it.multiple) {
                    pickMultipleMedia.launch(PickVisualMediaRequest(type))
                } else {
                    pickMedia.launch(PickVisualMediaRequest(type))
                }
            } else {
                doPickFile(it)
            }
        }

        receiveEvent<ExportFileEvent> { event ->
            exportFileType = event.type
            exportFileActivityLauncher.launch(
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_TITLE, event.fileName)
                },
            )
        }

        receiveEvent<ConfirmToAcceptLoginEvent> { event ->
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
                            R.string.client_ua, "os_name", r.osName.capitalize(), "os_version", r.osVersion, "browser_name", r.browserName.capitalize(), "browser_version", r.browserVersion,
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
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        lifecycleScope.launch(Dispatchers.IO) {
            Language.initLocaleAsync(this@MainActivity)
        }
    }

    private fun doPickFile(event: PickFileEvent) {
        pickFileActivityLauncher.launch(FilePickHelper.getPickFileIntent(event.multiple))
    }

    private fun showTermsAndPrivacyDialog(context: Context) {
        val message = "请您认真阅读《用户协议》和《隐私政策》的全部条款，接受后可开始使用我们的服务。"

        val startIndexUserAgreement = message.indexOf("《用户协议》")
        val startIndexPrivacyPolicy = message.indexOf("《隐私政策》")

        val spannableString = SpannableString(message)
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                WebHelper.open(context, UrlHelper.getTermsUrl())
            }
        }, startIndexUserAgreement, startIndexUserAgreement + 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                WebHelper.open(context, UrlHelper.getPolicyUrl())
            }
        }, startIndexPrivacyPolicy, startIndexPrivacyPolicy + 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val dialog = AlertDialog.Builder(context)
            .setTitle("温馨提示")
            .setCancelable(false)
            .setPositiveButton("同意并继续") { _, _ ->
                coIO {
                    AgreeTermsPreference.putAsync(context, true)
                    doWhenReadyAsync()
                }
            }
            .setNegativeButton("不同意") { _, _ ->
                this@MainActivity.finish()
            }
            .create()

        dialog.setView(TextView(context).apply {
            text = spannableString
            movementMethod = LinkMovementMethod.getInstance()
        }, context.dp2px(24), context.dp2px(28), context.dp2px(24), context.dp2px(28))
        dialog.show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
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
                        DialogHelper.showMessage(LocaleHelper.getString(R.string.not_supported_error))
                    }
                } else {
                    DialogHelper.showMessage(LocaleHelper.getString(R.string.not_supported_error))
                }
            }
        } else if (intent.action == Intent.ACTION_SEND) {
            if (intent.type?.startsWith("text/") == true) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                coMain {
                    val item = withIO {
                        ChatHelper.sendAsync(DMessageContent(DMessageType.TEXT.value, DMessageText(sharedText)))
                    }
                    sendEvent(
                        WebSocketEvent(
                            EventType.MESSAGE_CREATED,
                            JsonHelper.jsonEncode(
                                arrayListOf(
                                    item.toModel().apply {
                                        data = this.getContentData()
                                    },
                                ),
                            ),
                        ),
                    )
                    navControllerState.value?.navigate(Routing.Chat)
                }
                return
            }

            val uri = intent.parcelable(Intent.EXTRA_STREAM) as? Uri ?: return
            DialogHelper.showConfirmDialog(
                "", LocaleHelper.getString(R.string.confirm_to_send_file_to_file_assistant),
                confirmButton = LocaleHelper.getString(R.string.ok) to {
                    navControllerState.value?.navigate(Routing.Chat)
                    coIO {
                        delay(1000)
                        sendEvent(PickFileResultEvent(PickFileTag.SEND_MESSAGE, PickFileType.FILE, setOf(uri)))
                    }
                },
                dismissButton = LocaleHelper.getString(R.string.cancel) to {})
        } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            DialogHelper.showConfirmDialog(
                "", LocaleHelper.getString(R.string.confirm_to_send_files_to_file_assistant),
                confirmButton = LocaleHelper.getString(R.string.ok) to {
                    val uris = intent.parcelableArrayList<Uri>(Intent.EXTRA_STREAM)
                    if (uris != null) {
                        navControllerState.value?.navigate(Routing.Chat)
                        coIO {
                            delay(1000)
                            sendEvent(PickFileResultEvent(PickFileTag.SEND_MESSAGE, PickFileType.FILE, uris.toSet()))
                        }
                    }
                },
                dismissButton = LocaleHelper.getString(R.string.cancel) to {})
        }
    }

    companion object {
        lateinit var instance: WeakReference<MainActivity>
    }
}
