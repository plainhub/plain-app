package com.ismartcoding.plain.ui.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ismartcoding.plain.R

object WebHelper {
    fun open(
        context: Context,
        url: String,
    ) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (ex: java.lang.Exception) {
            DialogHelper.showMessage(R.string.no_browser_error)
        }
    }
}
