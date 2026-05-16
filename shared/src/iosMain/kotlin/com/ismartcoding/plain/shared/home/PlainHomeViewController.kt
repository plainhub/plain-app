package com.ismartcoding.plain.shared.home

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun PlainHomeViewController(): UIViewController = ComposeUIViewController {
    PlainHomeScreen()
}
