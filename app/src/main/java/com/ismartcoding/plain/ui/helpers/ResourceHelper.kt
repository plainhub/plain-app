package com.ismartcoding.plain.ui.helpers

import android.content.Context
import com.ismartcoding.lib.extensions.getDrawableId

object ResourceHelper {
    fun getCurrencyFlagResId(
        context: Context,
        currencyCode: String,
    ): Int {
        return context.getDrawableId("cflag_" + currencyCode.lowercase())
    }
}
