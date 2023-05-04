package com.ismartcoding.plain.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.*
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.*
import com.ismartcoding.plain.data.enums.ExportFileType
import com.ismartcoding.plain.data.enums.PickFileTag
import com.ismartcoding.plain.data.enums.PickFileType
import com.ismartcoding.plain.databinding.ActivityMainBinding
import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.features.*
import com.ismartcoding.plain.features.bluetooth.BluetoothPermission
import com.ismartcoding.plain.features.box.BoxHelper
import com.ismartcoding.plain.features.box.CancelActiveJobEvent
import com.ismartcoding.plain.features.box.FetchInitDataEvent
import com.ismartcoding.plain.features.box.InitDataResultEvent
import com.ismartcoding.plain.features.chat.ChatHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.locale.LocaleHelper.getStringF
import com.ismartcoding.plain.features.theme.AppThemeHelper
import com.ismartcoding.plain.mediaProjectionManager
import com.ismartcoding.plain.services.ScreenMirrorService
import com.ismartcoding.plain.ui.chat.SendMessageDialog
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.FilePickHelper
import com.ismartcoding.plain.ui.models.ShowMessageEvent
import com.ismartcoding.plain.web.*
import com.ismartcoding.plain.web.websocket.EventType
import com.ismartcoding.plain.web.websocket.WebSocketEvent
import io.ktor.server.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var pickFileType = PickFileType.IMAGE
    private var pickFileTag = PickFileTag.SEND_MESSAGE
    private var exportFileType = ExportFileType.OPML

    private val screenCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            if (ScreenMirrorService.instance == null) {
                val metrics = resources.displayMetrics
                val service = Intent(this, ScreenMirrorService::class.java)
                service.putExtra("code", result.resultCode)
                service.putExtra("data", result.data!!)
                service.putExtra("width", metrics.widthPixels)
                service.putExtra("height", metrics.heightPixels)
                service.putExtra("density", metrics.densityDpi)
                startService(service)
            }
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
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

    override fun onBackPressed() {
        moveTaskToBack(false)
    }

    private fun initStatusBar() {
        immersionBar {
            transparentBar()
            titleBar(binding.topAppBar.toolbar)
            statusBarDarkFont(!AppThemeHelper.isDarkMode())
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        LocaleHelper.setLocale(this, LocalStorage.appLocale)

        instance = WeakReference(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initStatusBar()

        if (!isGestureNavigationBar()) {
            binding.page.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                bottomMargin = navigationBarHeight
            }
        }

        binding.topAppBar.quickNav.updatePadding(0, statusBarHeight, 0, 0)
        binding.topAppBar.autoStatusBar(window)

        BluetoothPermission.init(this)
        Permissions.init(this)
        initEvents()

        binding.fab.setSafeClick {
            SendMessageDialog().show()
        }

        binding.page.pageName = javaClass.simpleName
        binding.page.run {
//                onRefresh {
//                    finishRefresh()
//                }
            setEnableRefresh(false)
        }

        binding.chatList.initView(lifecycle)
        sendEvent(StartHttpServerEvent())
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshUIForThemeChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothPermission.release()
        Permissions.release()
    }

    override fun onResume() {
        super.onResume()
        // if there is any dialog in front of activity then ignore fetching init data. Case: when choosing file
        if (!supportFragmentManager.fragments.any { it is DialogFragment }) {
            lifecycleScope.launch {
                UIDataCache.current().box = withIO {
                    BoxHelper.getSelectedBoxAsync()
                }
                sendEvent(FetchInitDataEvent.createDefault())
                binding.topAppBar.mainRefresh()
                binding.chatList.refreshAsync()
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun initEvents() {
        receiveEvent<CurrentBoxChangedEvent> {
            lifecycleScope.launch {
                UIDataCache.current().clearConnectivityState()
                binding.topAppBar.mainRefresh()
                sendEvent(
                    CancelActiveJobEvent(
                        LocalStorage.selectedBoxId,
                        FetchInitDataEvent.javaClass
                    )
                )
                sendEvent(FetchInitDataEvent.createDefault())
                binding.chatList.refreshAsync()
            }
        }

        receiveEvent<HttpServerEnabledEvent> {
            binding.topAppBar.mainRefresh()
        }

        receiveEvent<InitDataResultEvent> {
            binding.topAppBar.mainRefresh()
        }

        receiveEvent<UpdateLocaleEvent> {
            refreshUIForLocaleChanged()
        }

        receiveEvent<ShowMessageEvent> { event ->
            Toast.makeText(instance.get()!!, event.message, event.duration).show()
        }

        receiveEvent<DeleteChatItemViewEvent> { event ->
            binding.chatList.remove(event.id)
            if (!binding.chatList.isScrollable()) {
                binding.fab.isVisible = true
            }
        }

        receiveEvent<SendMessageEvent> { event ->
            lifecycleScope.launch {
                val items = withIO { ChatHelper.createChatItemsAsync(event.content) }
                sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED))
                binding.chatList.addAll(items)
                DialogHelper.hideLoading()
            }
        }

        receiveEvent<HttpServerEvents.MessageCreatedEvent> { event ->
            binding.chatList.addAll(event.items)
        }

        receiveEvent<PermissionResultEvent> { event ->
            if (event.permission == Permission.WRITE_SETTINGS.toSysPermission() && Permission.WRITE_SETTINGS.can()) {
                val enable = !LocalStorage.keepScreenOn
                LocalStorage.keepScreenOn = enable
                if (enable) {
                    LocalStorage.systemScreenTimeout = contentResolver.getSystemScreenTimeout()
                    contentResolver.setSystemScreenTimeout(Int.MAX_VALUE)
                } else if (LocalStorage.systemScreenTimeout > 0) {
                    contentResolver.setSystemScreenTimeout(LocalStorage.systemScreenTimeout)
                }
                binding.topAppBar.mainRefresh()
            }
        }

        receiveEvent<StartScreenMirrorEvent> {
            screenCapture.launch(mediaProjectionManager.createScreenCaptureIntent())
        }

        receiveEvent<UpdateMessageEvent> { event ->
            lifecycleScope.launch {
                val update = ChatItemDataUpdate(event.chatItem.id, event.chatItem.content)
                withIO {
                    AppDatabase.instance.chatDao().updateData(update)
                }
                binding.chatList.update(event.chatItem)
            }
        }

        receiveEvent<BoxConnectivityStateChangedEvent> {
            binding.topAppBar.mainRefresh()
        }

        receiveEvent<EnableWebConsoleEvent> {
            binding.topAppBar.mainRefresh()
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
            val session = event.session
            val clientIp = HttpServerManager.clientIpCache[event.clientId] ?: ""
            MaterialAlertDialogBuilder(instance.get()!!)
                .setTitle(getStringF(R.string.request_to_connect, "ip", clientIp))
                .setMessage(
                    getStringF(
                        R.string.client_ua, "os_name", event.osName.capitalize(), "os_version",
                        event.osVersion, "browser_name", event.browserName.capitalize(), "browser_version", event.browserVersion
                    )
                )
                .setPositiveButton(getString(R.string.accept)) { _, _ ->
                    launch {
                        val token = CryptoHelper.generateAESKey()
                        withIO {
                            SessionList.addOrUpdateAsync(event.clientId) {
                                it.clientIP = clientIp
                                it.osName = event.osName
                                it.osVersion = event.osVersion
                                it.browserName = event.browserName
                                it.browserVersion = event.browserVersion
                                it.token = token
                            }
                            HttpServerManager.loadTokenCache()
                            session.send(
                                CryptoHelper.aesEncrypt(
                                    HttpServerManager.passwordToToken(), JsonHelper.jsonEncode(
                                        AuthResponse(
                                            AuthStatus.COMPLETED,
                                            token
                                        )
                                    )
                                )
                            )
                        }
                    }
                }
                .setNegativeButton(getString(R.string.reject)) { _, _ ->
                    launch {
                        withIO {
                            session.close(
                                CloseReason(
                                    CloseReason.Codes.TRY_AGAIN_LATER,
                                    "rejected"
                                )
                            )
                        }
                    }
                }
                .create()
                .show()
        }

    }

    private fun doPickFile(event: PickFileEvent) {
        pickFileActivityLauncher.launch(FilePickHelper.getPickFileIntent(event.type, event.multiple))
    }

    private fun refreshUIForThemeChanged() {
        initStatusBar()
        val context = this
        window.decorView.setBackgroundColor(context.getColor(R.color.canvas))
        binding.topAppBar.mainRefresh()
        binding.topAppBar.refreshUI()
        binding.chatList.refreshUI()
    }

    private fun refreshUIForLocaleChanged() {
        binding.topAppBar.mainRefresh()
        binding.chatList.refreshUI()
    }

    companion object {
        lateinit var instance: WeakReference<MainActivity>
    }
}