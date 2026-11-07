package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.ui.navigation.LauncherShortcutTools
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationShortcutIcon
import platform.UIKit.UIApplicationShortcutItem
import platform.UIKit.shortcutItems

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun publishLauncherShortcuts(tools: List<AppFeatureType>) {
    UIApplication.sharedApplication.shortcutItems = tools.map { type ->
        val symbol = when (type) {
            AppFeatureType.NOTES -> "note.text"
            AppFeatureType.DOCS -> "doc.text"
            AppFeatureType.FEEDS -> "dot.radiowaves.up.forward"
            AppFeatureType.POMODORO_TIMER -> "timer"
            AppFeatureType.IMAGES -> "photo"
            AppFeatureType.VIDEOS -> "video"
            AppFeatureType.AUDIO -> "music.note"
            AppFeatureType.FILES -> "folder"
            else -> error("Unsupported launcher shortcut: $type")
        }
        UIApplicationShortcutItem(
            type = type.name,
            localizedTitle = LocaleHelper.getStringAsync(LauncherShortcutTools.featureItem(type).titleRes),
            localizedSubtitle = null,
            icon = UIApplicationShortcutIcon.iconWithSystemImageName(symbol),
            userInfo = null,
        )
    }
}
