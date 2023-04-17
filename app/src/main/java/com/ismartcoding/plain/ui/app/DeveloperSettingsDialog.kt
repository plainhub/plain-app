package com.ismartcoding.plain.ui.app

import android.os.Bundle
import android.view.View
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogDeveloperSettingsBinding
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.*

class DeveloperSettingsDialog : BaseBottomSheetDialog<DialogDeveloperSettingsBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (LocalStorage.authDevToken.isEmpty()) {
            LocalStorage.resetAuthDevToken()
        }
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

        binding.apiUrl.setKeyText(R.string.api_url)
            .addTextRow("https://${NetworkHelper.getDeviceIP4()}:8443/graphql")
            .addTextRow("http://${NetworkHelper.getDeviceIP4()}:8080/graphql")

        binding.tips.text = getString(R.string.auth_dev_token_tips)
    }
}