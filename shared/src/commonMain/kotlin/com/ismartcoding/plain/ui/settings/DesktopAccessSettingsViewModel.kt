package com.ismartcoding.plain.ui.settings

import com.ismartcoding.plain.platform.network.NetworkEvents
import com.ismartcoding.plain.HttpServerManager

import com.ismartcoding.plain.ui.state.launchSafe

import com.ismartcoding.plain.ui.i18n.LocaleHelper

import com.ismartcoding.plain.i18n.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.rust.native.RustNativeApi
import com.ismartcoding.plain.ui.components.DialogHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

class DesktopAccessSettingsViewModel : ViewModel() {
    val lanIpv4Addresses = MutableStateFlow<List<String>?>(null)

    suspend fun observeNetworkAddresses() {
        try {
            NetworkEvents.refresh()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            LogCat.e("Load network addresses", error)
        }
        NetworkEvents.snapshot.collect { snapshot ->
            lanIpv4Addresses.value = snapshot.lanIpv4Addresses
        }
    }

    fun checkHttpServerHealth() {
        viewModelScope.launchSafe {
            DialogHelper.showLoading()
            val serverUp = try {
                RustNativeApi.checkHttp()
            } finally {
                DialogHelper.hideLoading()
            }
            if (!serverUp) {
                DialogHelper.showConfirmDialog(
                    LocaleHelper.getStringAsync(Res.string.error),
                    LocaleHelper.getStringAsync(Res.string.http_server_error),
                    confirmButton = Pair(LocaleHelper.getStringAsync(Res.string.ok)) {},
                    dismissButton = Pair(LocaleHelper.getStringAsync(Res.string.retry)) { HttpServerManager.restartHttpServer() },
                )
            } else {
                DialogHelper.showConfirmDialog(LocaleHelper.getStringAsync(Res.string.confirm), LocaleHelper.getStringAsync(Res.string.http_server_ok))
            }
        }
    }
}
