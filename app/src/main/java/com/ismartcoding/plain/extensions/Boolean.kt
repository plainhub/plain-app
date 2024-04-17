package com.ismartcoding.plain.extensions

import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString

fun Boolean.getText(): String {
    return if (this) getString(R.string.on) else getString(R.string.off)
}

fun Boolean.toJsValue(): String {
    return if (this) "true" else "false"
}
