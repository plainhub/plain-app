package com.ismartcoding.plain.ui.nav

import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.page.home.FeatureItem

/** Candidate tools for the launcher long-press shortcuts (published as dynamic shortcuts). */
object LauncherShortcutTools {
    // List order = publish rank; launchers show the leading entries first.
    // Identified by AppFeatureType only — never invent parallel string ids.
    val ALL = listOf(
        AppFeatureType.NOTES, AppFeatureType.DOCS, AppFeatureType.FEEDS, AppFeatureType.POMODORO_TIMER,
        AppFeatureType.IMAGES, AppFeatureType.VIDEOS, AppFeatureType.AUDIO, AppFeatureType.FILES,
    )
    const val MAX_SELECTED = 4
    val DEFAULT = listOf(
        AppFeatureType.NOTES, AppFeatureType.DOCS, AppFeatureType.FEEDS, AppFeatureType.POMODORO_TIMER,
    )

    fun featureItem(type: AppFeatureType): FeatureItem = when (type) {
        AppFeatureType.NOTES -> FeatureItem(type, Res.string.notes, Res.drawable.notebook_pen) {}
        AppFeatureType.DOCS -> FeatureItem(type, Res.string.docs, Res.drawable.file_text) {}
        AppFeatureType.FEEDS -> FeatureItem(type, Res.string.feeds, Res.drawable.rss) {}
        AppFeatureType.POMODORO_TIMER -> FeatureItem(type, Res.string.pomodoro_timer, Res.drawable.timer) {}
        AppFeatureType.IMAGES -> FeatureItem(type, Res.string.images, Res.drawable.image) {}
        AppFeatureType.VIDEOS -> FeatureItem(type, Res.string.videos, Res.drawable.video) {}
        AppFeatureType.AUDIO -> FeatureItem(type, Res.string.audios, Res.drawable.music) {}
        AppFeatureType.FILES -> FeatureItem(type, Res.string.files, Res.drawable.folder) {}
        else -> FeatureItem(type, Res.string.tools, Res.drawable.layout_grid) {}
    }
}
