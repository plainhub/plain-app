package com.ismartcoding.plain.platform

import androidx.compose.ui.text.TextMeasurer
import com.ismartcoding.plain.lib.logcat.LogCat
import platform.UIKit.UISimpleTextPrintFormatter
import platform.UIKit.UIPrintInfo
import platform.UIKit.UIPrintInteractionController
import platform.UIKit.UIViewController

actual fun printText(textMeasurer: TextMeasurer, jobName: String, content: String) {
    val controller = UIPrintInteractionController.sharedPrintController() ?: run {
        LogCat.e("printText: sharedPrintController unavailable")
        return
    }
    val info = UIPrintInfo.printInfo() ?: run {
        LogCat.e("printText: printInfo unavailable")
        return
    }
    info.jobName = jobName
    controller.printInfo = info
    val formatter = UISimpleTextPrintFormatter(content)
    formatter.startPage = 0
    controller.printFormatter = formatter
    val rootVc = rootViewController() ?: run {
        LogCat.e("printText: no root UIViewController available")
        return
    }
    controller.presentAnimated(true, completionHandler = { _, completed, error ->
        if (error != null) {
            LogCat.e("printText: ${error.localizedDescription}")
        } else if (completed) {
            LogCat.d("printText: completed")
        }
    })
}

private fun rootViewController(): UIViewController? {
    val window = platform.UIKit.UIApplication.sharedApplication.keyWindow ?: return null
    return window.rootViewController
}
