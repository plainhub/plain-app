package com.ismartcoding.plain.ui.app

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.data.enums.PasswordType
import com.ismartcoding.plain.databinding.DialogHttpServerPasswordSettingsBinding
import com.ismartcoding.plain.features.HttpServerPasswordChangedEvent
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.initView
import com.ismartcoding.plain.ui.views.ChipItem
import com.ismartcoding.plain.web.HttpServerManager


class HttpServerPasswordSettingsDialog() : BaseBottomSheetDialog<DialogHttpServerPasswordSettingsBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.type.initView(ChipItem.getPasswordTypes(), LocalStorage.httpServerPasswordType.name, false) {
            LocalStorage.httpServerPasswordType = PasswordType.valueOf(it)
            updateByType(LocalStorage.httpServerPasswordType)
            sendEvent(HttpServerPasswordChangedEvent())
        }
        updateByType(LocalStorage.httpServerPasswordType)
    }

    private fun updateByType(type: PasswordType) {
        when (type) {
            PasswordType.RANDOM -> {
                binding.passwordView.isVisible = true
                binding.passwordView.text = LocalStorage.httpServerPassword
                binding.passwordView.isEnabled = false
                binding.passwordView.setEndIconOnClick {
                    HttpServerManager.resetPassword()
                    sendEvent(HttpServerPasswordChangedEvent())
                    binding.passwordView.text = LocalStorage.httpServerPassword
                }
                binding.password.isVisible = false
            }
            PasswordType.FIXED -> {
                binding.passwordView.isVisible = false
                binding.password.text = LocalStorage.httpServerPassword
                binding.password.onTextChanged = {
                    if (it.isNotEmpty()) {
                        LocalStorage.httpServerPassword = it
                        sendEvent(HttpServerPasswordChangedEvent())
                    }
                }
                binding.password.isVisible = true
            }
            else -> {
                binding.passwordView.isVisible = false
                binding.password.isVisible = false
            }
        }
    }
}