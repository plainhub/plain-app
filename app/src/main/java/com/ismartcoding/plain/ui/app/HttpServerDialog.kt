package com.ismartcoding.plain.ui.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.core.view.isVisible
import com.ismartcoding.lib.brv.utils.*
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.*
import com.ismartcoding.plain.data.enums.PasswordType
import com.ismartcoding.plain.databinding.DialogHttpServerBinding
import com.ismartcoding.plain.databinding.ItemHttpServerHeaderBinding
import com.ismartcoding.plain.databinding.ItemRowBinding
import com.ismartcoding.plain.features.*
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ListItemModel
import com.ismartcoding.plain.ui.models.RvHeader
import com.ismartcoding.plain.ui.views.ChipItem
import com.ismartcoding.plain.web.HttpServerManager

class HttpServerDialog : BaseDialog<DialogHttpServerBinding>() {
    data class PermissionModel(val data: Permission) : ListItemModel()

    private val header = RvHeader()
    private fun updateNotification() {
        if (MainApp.instance.httpServerError.isNotEmpty()) {
            binding.topAppBar.showNotification(MainApp.instance.httpServerError, R.color.red)
        } else {
            binding.topAppBar.hideNotification()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.setScrollBehavior(false)
        binding.topAppBar.toolbar.run {
            setTitle(R.string.web_console)
            onBack {
                dismiss()
            }
            initMenu(R.menu.http_server, overflow = true)
            onMenuItemClick {
                when (itemId) {
                    R.id.signature -> {
                        val context = requireContext()
                        DialogHelper.showConfirmDialog(
                            context,
                            getString(R.string.https_certificate_signature),
                            HttpServerManager.getSSLSignature(context).joinToString(" ") { "%02x".format(it).uppercase() })
                    }
                    R.id.sessions -> {
                        SessionsDialog().show()
                    }
                    R.id.settings -> {
                        HttpServerSettingsDialog().show()
                    }
                }
            }
        }


        binding.list.rv.linear().setup {
            addType<RvHeader>(R.layout.item_http_server_header)
            addType<PermissionModel>(R.layout.item_row)
            onBind {
                if (itemViewType == R.layout.item_http_server_header) {
                    val b = getBinding<ItemHttpServerHeaderBinding>()
                    b.enable.setSwitch(LocalStorage.webConsoleEnabled) { _, isEnabled ->
                        LocalStorage.webConsoleEnabled = isEnabled
                        sendEvent(HttpServerEnabledEvent(isEnabled))
                        if (isEnabled) {
                            requestIgnoreBatteryOptimization()
                            sendEvent(StartHttpServerEvent())
                        }
                    }
                    b.password.run {
                        setValueText(
                            if (LocalStorage.httpServerPasswordType != PasswordType.NONE) LocalStorage.httpServerPassword + " (" + LocalStorage.httpServerPasswordType.getText() + ")" else getString(
                                R.string.password_type_none
                            )
                        )
                        showMore()
                        setClick {
                            HttpServerPasswordSettingsDialog {
                                setValueText(
                                    if (LocalStorage.httpServerPasswordType != PasswordType.NONE) LocalStorage.httpServerPassword + " (" + LocalStorage.httpServerPasswordType.getText() + ")" else getString(
                                        R.string.password_type_none
                                    )
                                )
                            }.show()
                        }
                    }

                    b.types.initView(listOf(ChipItem(getString(R.string.recommended_https), "https"), ChipItem("HTTP", "http")), "https") { v ->
                        b.url.text = if (v == "https") {
                            "https://${NetworkHelper.getDeviceIP4().ifEmpty { "127.0.0.1" }}:${LocalStorage.httpsPort}"
                        } else {
                            "http://${NetworkHelper.getDeviceIP4().ifEmpty { "127.0.0.1" }}:${LocalStorage.httpPort}"
                        }
                        b.tips.isVisible = v == "https"
                    }

                    b.url.text = "https://${NetworkHelper.getDeviceIP4().ifEmpty { "127.0.0.1" }}:${LocalStorage.httpsPort}"
                    b.tips.text = LocaleHelper.getString(R.string.open_website_on_desktop)
                } else {
                    val b = getBinding<ItemRowBinding>()
                    val m = getModel<PermissionModel>()
                    if (m.data != Permission.NONE) {
                        b.setSwitch(m.data.isEnabled(), onChanged = { _, isEnabled ->
                            m.data.setEnabled(isEnabled)
                            if (isEnabled) {
                                m.data.grant()
                            }
                        })
                    }
                    b.container.setSafeClick {
                        if (m.data == Permission.NONE) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", requireContext().packageName, null)
                            startActivity(intent)
                        }
                    }
                }
            }
        }
        binding.list.rv.bindingAdapter.addHeader(header)

        binding.list.page.onRefresh {
            updateUI()
            updateNotification()
        }.showLoading()

        receiveEvent<PermissionResultEvent> {
            binding.list.page.refresh()
        }

        receiveEvent<HttpServerEnabledEvent> {
            binding.list.page.refresh()
        }

        updateNotification()
    }

    private fun updateUI() {
        binding.list.page.addData(Permissions.getWebList(requireContext()).map {
            PermissionModel(it).apply {
                keyText = it.getText()
                if (it == Permission.NONE) {
                    showSwitch = false
                    subtitle = ""
                    showMore(true)
                } else {
                    showSwitch = true
                    subtitle = getString(if (it.can()) R.string.system_permission_granted else R.string.system_permission_not_granted)
                    showMore(false)
                }
            }
        })
    }

    private fun requestIgnoreBatteryOptimization() {
        try {
            val packageName = BuildConfig.APPLICATION_ID
            val pm = requireContext().getSystemService(Application.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent()
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}

