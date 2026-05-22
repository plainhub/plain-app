package com.ismartcoding.plain.extensions

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun Boolean.getText(): String {
    return if (this) stringResource(Res.string.on) else stringResource(Res.string.off)
}

fun Boolean.toJsValue(): String {
    return if (this) "true" else "false"
}
