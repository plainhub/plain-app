package com.ismartcoding.plain.ui.app

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogAboutBinding
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.setKeyText
import com.ismartcoding.plain.ui.extensions.setSwitch
import com.ismartcoding.plain.ui.extensions.setValueText

class AboutDialog : BaseBottomSheetDialog<DialogAboutBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.clientId
            .setKeyText(R.string.client_id)
            .setValueText(LocalStorage.clientId)

        binding.appVersion
            .setKeyText(R.string.app_version)
            .setValueText(MainApp.getAppVersion())

        binding.androidVersion.setKeyText(R.string.android_version)
            .setValueText(MainApp.getAndroidVersion())

        if (BuildConfig.DEBUG) {
            binding.demoMode
                .setKeyText(R.string.demo_mode)
                .setSwitch(LocalStorage.demoMode) { _, isEnabled ->
                    LocalStorage.demoMode = isEnabled
                }
        } else {
            binding.demoMode.root.isVisible = false
        }
    }
}