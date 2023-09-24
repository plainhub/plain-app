package com.ismartcoding.plain.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.database.CursorWindow
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.*
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.*
import com.ismartcoding.plain.data.enums.ExportFileType
import com.ismartcoding.plain.data.enums.Language
import com.ismartcoding.plain.data.enums.PickFileTag
import com.ismartcoding.plain.data.enums.PickFileType
import com.ismartcoding.plain.data.preference.KeepScreenOnPreference
import com.ismartcoding.plain.data.preference.SettingsProvider
import com.ismartcoding.plain.data.preference.SystemScreenTimeoutPreference
import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.features.*
import com.ismartcoding.plain.features.bluetooth.BluetoothPermission
import com.ismartcoding.plain.features.locale.LocaleHelper.getStringF
import com.ismartcoding.plain.helpers.ScreenHelper
import com.ismartcoding.plain.mediaProjectionManager
import com.ismartcoding.plain.services.ScreenMirrorService
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.helpers.FilePickHelper
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.ShowMessageEvent
import com.ismartcoding.plain.ui.page.Main
import com.ismartcoding.plain.web.*
import com.ismartcoding.plain.web.websocket.EventType
import com.ismartcoding.plain.web.websocket.WebSocketEvent
import io.ktor.server.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private var pickFileType = PickFileType.IMAGE
    private var pickFileTag = PickFileTag.SEND_MESSAGE
    private var exportFileType = ExportFileType.OPML
    private var requestToConnectDialog: AlertDialog? = null
    private val viewModel: MainViewModel by viewModels()
    private val screenCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val base = ScreenMirrorService.instance?.getLatestImageBase64()
            if (ScreenMirrorService.instance == null || base.isNullOrEmpty()) {
                val service = Intent(this, ScreenMirrorService::class.java)
                service.putExtra("code", result.resultCode)
                service.putExtra("data", result.data)
                startService(service)
            } else {
                sendEvent(WebSocketEvent(EventType.SCREEN_MIRRORING, base, false))
            }
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            sendEvent(PickFileResultEvent(pickFileTag, pickFileType, setOf(uri)))
        }
    }

    private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
            sendEvent(PickFileResultEvent(pickFileTag, pickFileType, uris.toSet()))
        }
    }

    private val pickFileActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            sendEvent(PickFileResultEvent(pickFileTag, pickFileType, FilePickHelper.getUris(result.data!!)))
        }
    }

    private val exportFileActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data?.data != null) {
                sendEvent(ExportFileResultEvent(exportFileType, data.data!!))
            }
        }
    }

    private fun fixSystemBarsAnimation() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    @SuppressLint("ClickableViewAccessibility", "DiscouragedPrivateApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
//        WindowCompat.setDecorFitsSystemWindows(window, false)
        lifecycleScope.launch(Dispatchers.IO) {
            Language.initLocaleAsync(this@MainActivity)
        }
        fixSystemBarsAnimation()

        instance = WeakReference(this)

        // https://stackoverflow.com/questions/51959944/sqliteblobtoobigexception-row-too-big-to-fit-into-cursorwindow-requiredpos-0-t
        try {
            val field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
            field.isAccessible = true
            field.set(null, 100 * 1024 * 1024) //the 100MB is the new size
        } catch (e: Exception) {
            e.printStackTrace()
        }

        BluetoothPermission.init(this)
        Permissions.init(this)
        initEvents()

        setContent {
            SettingsProvider {
                Main(viewModel)
            }
        }

        Permissions.checkNotification(this@MainActivity, R.string.foreground_service_notification_prompt) {
            sendEvent(StartHttpServerEvent())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothPermission.release()
        Permissions.release()
    }

    @SuppressLint("CheckResult")
    private fun initEvents() {
        receiveEvent<ShowMessageEvent> { event ->
            Toast.makeText(instance.get()!!, event.message, event.duration).show()
        }

        receiveEvent<PermissionResultEvent> { event ->
            if (event.permission == Permission.WRITE_SETTINGS && Permission.WRITE_SETTINGS.can(this@MainActivity)) {
                val enable = !KeepScreenOnPreference.getAsync(this@MainActivity)
                ScreenHelper.saveOn(this@MainActivity, enable)
                if (enable) {
                    ScreenHelper.saveTimeout(this@MainActivity, contentResolver.getSystemScreenTimeout())
                    contentResolver.setSystemScreenTimeout(Int.MAX_VALUE)
                } else {
                    val systemScreenTimeout = SystemScreenTimeoutPreference.getAsync(this@MainActivity)
                    contentResolver.setSystemScreenTimeout(if (systemScreenTimeout > 0) systemScreenTimeout else 5000 * 60) // default 5 minutes
                }
            }
        }

        receiveEvent<StartScreenMirrorEvent> {
            screenCapture.launch(mediaProjectionManager.createScreenCaptureIntent())
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
            exportFileActivityLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "text/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, event.fileName)
            })
        }

        receiveEvent<ConfirmToAcceptLoginEvent> { event ->
            val clientIp = HttpServerManager.clientIpCache[event.clientId] ?: ""
            if (requestToConnectDialog?.isShowing == true) {
                requestToConnectDialog?.dismiss()
                requestToConnectDialog = null
            }

            val r = event.request
            requestToConnectDialog = MaterialAlertDialogBuilder(instance.get()!!).setTitle(getStringF(R.string.request_to_connect, "ip", clientIp)).setMessage(
                getStringF(
                    R.string.client_ua, "os_name", r.osName.capitalize(), "os_version", r.osVersion, "browser_name", r.browserName.capitalize(), "browser_version", r.browserVersion
                )
            ).setPositiveButton(getString(R.string.accept)) { _, _ ->
                launch {
                    withIO { HttpServerManager.respondTokenAsync(event, clientIp) }
                }
            }.setNegativeButton(getString(R.string.reject)) { _, _ ->
                launch {
                    withIO {
                        event.session.close(
                            CloseReason(
                                CloseReason.Codes.TRY_AGAIN_LATER, "rejected"
                            )
                        )
                    }
                }
            }.create()
            if (Permission.SYSTEM_ALERT_WINDOW.can(this@MainActivity)) {
                requestToConnectDialog?.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            }
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

    companion object {
        lateinit var instance: WeakReference<MainActivity>
    }
}