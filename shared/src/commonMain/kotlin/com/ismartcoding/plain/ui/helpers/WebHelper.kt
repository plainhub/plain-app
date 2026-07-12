package com.ismartcoding.plain.ui.helpers

import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.launchUrl

object WebHelper {
    fun open(url: String) {
        try {
            launchUrl(url)
        } catch (ex: Exception) {
            DialogHelper.showMessage(Res.string.no_browser_error)
        }
    }
}
