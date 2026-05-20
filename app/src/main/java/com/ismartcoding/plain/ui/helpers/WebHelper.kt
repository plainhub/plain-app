package com.ismartcoding.plain.ui.helpers

import com.ismartcoding.plain.i18n.*

import android.content.Context
import android.content.Intent
import android.net.Uri

object WebHelper {
    fun open(
        context: Context,
        url: String,
    ) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (ex: java.lang.Exception) {
            DialogHelper.showMessage(Res.string.no_browser_error)
        }
    }
}
