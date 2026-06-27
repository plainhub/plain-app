package com.ismartcoding.plain.ui.helpers

import com.ismartcoding.plain.i18n.*

import android.content.Context
import com.ismartcoding.plain.platform.launchUrl

object WebHelper {
    fun open(
        context: Context,
        url: String,
    ) {
        try {
            launchUrl(url)
        } catch (ex: java.lang.Exception) {
            DialogHelper.showMessage(Res.string.no_browser_error)
        }
    }
}
