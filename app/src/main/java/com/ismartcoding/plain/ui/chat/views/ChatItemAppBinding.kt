package com.ismartcoding.plain.ui.chat.views

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.ChatItemAppBinding
import com.ismartcoding.plain.features.UpdateLocaleEvent
import com.ismartcoding.plain.features.locale.AppLocale
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.theme.AppTheme
import com.ismartcoding.plain.features.theme.AppThemeHelper
import com.ismartcoding.plain.ui.MainActivity
import com.ismartcoding.plain.ui.SelectOptionsDialog
import com.ismartcoding.plain.ui.app.AboutDialog
import com.ismartcoding.plain.ui.app.BackupRestoreDialog
import com.ismartcoding.plain.ui.extensions.*

fun ChatItemAppBinding.initView() {
    theme.initTheme()
        .setKeyText(R.string.theme)
        .setValueText(LocalStorage.appTheme.getText())
        .showMore()
        .setClick {
            SelectOptionsDialog(LocaleHelper.getString(R.string.theme), AppTheme.values().toList()) {
                AppThemeHelper.setDarkMode(it)
                initView()
            }.show()
        }

    language.initTheme()
        .setKeyText(R.string.language)
        .setValueText(AppLocale(LocalStorage.appLocale).getText())
        .showMore()
        .setClick {
            SelectOptionsDialog(LocaleHelper.getString(R.string.language), LocaleHelper.getSelectItems()) {
                LocalStorage.appLocale = it.value
                MainActivity.instance.get()?.let {  a ->
                    LocaleHelper.setLocale(a, it.value)
                    sendEvent(UpdateLocaleEvent())
                    initView()
                }
            }.show()
        }

    backupRestore.initTheme()
        .setKeyText(R.string.backup_restore)
        .showMore()
        .setClick {
            BackupRestoreDialog().show()
        }

    about.initTheme()
        .setKeyText(R.string.about)
        .showMore()
        .setClick {
            AboutDialog().show()
        }
}
