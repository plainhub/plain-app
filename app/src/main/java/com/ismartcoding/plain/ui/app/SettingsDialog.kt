package com.ismartcoding.plain.ui.app

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogSettingsBinding
import com.ismartcoding.plain.features.UpdateLocaleEvent
import com.ismartcoding.plain.features.locale.AppLocale
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.theme.AppTheme
import com.ismartcoding.plain.features.theme.AppThemeHelper
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.MainActivity
import com.ismartcoding.plain.ui.SelectOptionsDialog
import com.ismartcoding.plain.ui.extensions.*


class SettingsDialog : BaseBottomSheetDialog<DialogSettingsBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
    }

    private fun updateUI() {
        val context = requireContext()
        val backgroundTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.canvas))
        (view?.parent as? View)?.let {
            ViewCompat.setBackgroundTintList(it, backgroundTint)
        }
        binding.topAppBar.setTitleTextColor(ContextCompat.getColor(context, R.color.primary))
        binding.topAppBar.title = getString(R.string.settings)
        binding.theme.initTheme()
            .setKeyText(R.string.theme)
            .setValueText(LocalStorage.appTheme.getText())
            .showMore()
            .setClick {
                SelectOptionsDialog(LocaleHelper.getString(R.string.theme), AppTheme.values().toList()) {
                    AppThemeHelper.setDarkMode(it)
                    updateUI()
                }.show()
            }

        binding.language.initTheme()
            .setKeyText(R.string.language)
            .setValueText(AppLocale(LocalStorage.appLocale).getText())
            .showMore()
            .setClick {
                SelectOptionsDialog(LocaleHelper.getString(R.string.language), LocaleHelper.getSelectItems()) {
                    LocalStorage.appLocale = it.value
                    MainActivity.instance.get()?.let { a ->
                        LocaleHelper.setLocale(a, it.value)
                        sendEvent(UpdateLocaleEvent())
                        updateUI()
                    }
                }.show()
            }

        binding.backupRestore.initTheme()
            .setKeyText(R.string.backup_restore)
            .showMore()
            .setClick {
                BackupRestoreDialog().show()
            }

        binding.about.initTheme()
            .setKeyText(R.string.about)
            .showMore()
            .setClick {
                AboutDialog().show()
            }
    }
}