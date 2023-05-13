package com.ismartcoding.plain.ui.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogDeveloperSettingsBinding
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.helpers.DialogHelper


class SettingsDialog : BaseBottomSheetDialog<DialogDeveloperSettingsBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (LocalStorage.authDevToken.isEmpty()) {
            LocalStorage.resetAuthDevToken()
        }

        updateHttpServerPortUI()

        binding.enable.setKeyText(R.string.enable_testing_token)
            .setSwitch(LocalStorage.authDevTokenEnabled) { _, isEnabled ->
                LocalStorage.authDevTokenEnabled = isEnabled
            }

        binding.token.setKeyText(R.string.testing_token)
            .addTextRow(LocalStorage.authDevToken)
            .enableSwipeMenu(true)
            .setLeftSwipeButton(getString(R.string.reset)) {
                LocalStorage.resetAuthDevToken()
                binding.token.clearTextRows()
                binding.token.addTextRow(LocalStorage.authDevToken)
            }

        updateApiUrlUI()
        binding.tips.text = getString(R.string.auth_dev_token_tips)
    }

    private fun updateApiUrlUI() {
        binding.apiUrl.setKeyText(R.string.api_url)
            .clearTextRows()
            .addTextRow("https://${NetworkHelper.getDeviceIP4()}:${LocalStorage.httpsPort}/graphql")
            .addTextRow("http://${NetworkHelper.getDeviceIP4()}:${LocalStorage.httpPort}/graphql")
    }

    private fun updateHttpServerPortUI() {
        binding.httpPort.setKeyText(R.string.http_port)
            .setSpinners(listOf("8080", "8180", "8280", "8380", "8480", "8580", "8680", "8780", "8880", "8980"), LocalStorage.httpPort.toString()) {
                LocalStorage.httpPort = it.toInt()
                DialogHelper.showConfirmDialog(requireContext(), getString(R.string.restart_app_title), getString(R.string.restart_app_message)) {
                    triggerRebirth(requireContext())
                }
            }

        binding.httpsPort.setKeyText(R.string.https_port)
            .setSpinners(listOf("8043", "8143", "8243", "8343", "8443", "8543", "8643", "8743", "8843", "8943"), LocalStorage.httpsPort.toString()) {
                LocalStorage.httpsPort = it.toInt()
                DialogHelper.showConfirmDialog(requireContext(), getString(R.string.restart_app_title), getString(R.string.restart_app_message)) {
                    triggerRebirth(requireContext())
                }
            }
    }

    private fun triggerRebirth(context: Context) {
        val packageManager: PackageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }
}