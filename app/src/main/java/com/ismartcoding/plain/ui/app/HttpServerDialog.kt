package com.ismartcoding.plain.ui.app

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
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
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
import com.ismartcoding.plain.wifiManager

class HttpServerDialog : BaseDialog<DialogHttpServerBinding>() {
    data class PermissionModel(val data: Permission) : ListItemModel()
    private val header = RvHeader()
    private fun updateNotification() {
        binding.list.rv.bindingAdapter.removeHeader(header)
        if (wifiManager.isWifiEnabled) {
            binding.topAppBar.notification.isVisible = false
            binding.list.rv.bindingAdapter.addHeader(header)
        } else {
            binding.topAppBar.notification.isVisible = true
            binding.topAppBar.notification.setBackgroundColor(requireContext().getColor(R.color.yellow))
            binding.topAppBar.notification.text = getString(R.string.wifi_required)
        }
    }

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
                        DialogHelper.showConfirmDialog(
                            requireContext(),
                            getString(R.string.https_certificate_signature),
                            HttpServerManager.getSSLSignature(requireContext()).joinToString(" ") { "%02x".format(it).uppercase() })
                    }
                    R.id.sessions -> {
                        SessionsDialog().show()
                    }
                    R.id.developer_settings -> {
                        DeveloperSettingsDialog().show()
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
                        }
                    }
                    b.password.run {
                        setValueText(HttpServerManager.password)
                        setPasswordMode()
                        enableSwipeButton(true)
                        setLeftSwipeButton(getString(R.string.reset)) {
                            HttpServerManager.resetPassword()
                            setValueText(HttpServerManager.password)
                        }
                    }

                    b.types.initView(listOf(ChipItem(getString(R.string.recommended_https), "https"), ChipItem("HTTP", "http")), "https") { v ->
                        b.url.text = if (v == "https") {
                            "https://${NetworkHelper.getDeviceIP4()}:8443"
                        } else {
                            "http://${NetworkHelper.getDeviceIP4()}:8080"
                        }
                        b.tips.isVisible = v == "https"
                    }

                    b.url.text = "https://${NetworkHelper.getDeviceIP4()}:8443"
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

        binding.list.page.onRefresh {
            updateUI()
            updateNotification()
        }.showLoading()

        receiveEvent<PermissionResultEvent> {
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
        val packageName = BuildConfig.APPLICATION_ID
        val pm = requireContext().getSystemService(Application.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent()
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }
}

