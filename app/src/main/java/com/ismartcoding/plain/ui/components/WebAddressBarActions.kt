package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.i18n.*
import android.content.Context

fun showRestartAppDialog(context: Context) {
    androidx.appcompat.app.AlertDialog.Builder(context)
        .setTitle(LocaleHelper.getString(Res.string.restart_app_title))
        .setMessage(LocaleHelper.getString(Res.string.restart_app_message))
        .setPositiveButton(LocaleHelper.getString(Res.string.relaunch_app)) { _, _ ->
            AppHelper.relaunch(context)
        }
        .setCancelable(false)
        .create()
        .show()
}