package com.ismartcoding.plain.ui.extensions

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun rememberWindowInsetsController(): WindowInsetsControllerCompat {
    val window = with(LocalActivity.current as Activity) { return@with window }
    return remember { WindowCompat.getInsetsController(window, window.decorView) }
}