package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.ui.navigation.LauncherShortcutTools

internal actual suspend fun publishLauncherShortcuts(tools: List<AppFeatureType>) {
    val shortcuts = tools.map { type ->
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
        IosLauncherShortcut(
            type = type.name,
            title = LocaleHelper.getStringAsync(LauncherShortcutTools.featureItem(type).titleRes),
            symbol = symbol,
        )
    }
    IosPlatformRegistry.publishLauncherShortcuts(shortcuts)
}
