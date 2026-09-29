package com.ismartcoding.plain.ui.nav

import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.page.home.FeatureItem
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.file_text as ui_drawable_file_text
import com.ismartcoding.plain.ui.resources.folder as ui_drawable_folder
import com.ismartcoding.plain.ui.resources.image as ui_drawable_image
import com.ismartcoding.plain.ui.resources.layout_grid as ui_drawable_layout_grid
import com.ismartcoding.plain.ui.resources.music as ui_drawable_music
import com.ismartcoding.plain.ui.resources.notebook_pen as ui_drawable_notebook_pen
import com.ismartcoding.plain.ui.resources.rss as ui_drawable_rss
import com.ismartcoding.plain.ui.resources.timer as ui_drawable_timer
import com.ismartcoding.plain.ui.resources.video as ui_drawable_video
import com.ismartcoding.plain.i18n.folder
import com.ismartcoding.plain.i18n.video
import com.ismartcoding.plain.i18n.image

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
        AppFeatureType.NOTES -> FeatureItem(type, Res.string.notes, UiRes.drawable.ui_drawable_notebook_pen) {}
        AppFeatureType.DOCS -> FeatureItem(type, Res.string.docs, UiRes.drawable.ui_drawable_file_text) {}
        AppFeatureType.FEEDS -> FeatureItem(type, Res.string.feeds, UiRes.drawable.ui_drawable_rss) {}
        AppFeatureType.POMODORO_TIMER -> FeatureItem(type, Res.string.pomodoro_timer, UiRes.drawable.ui_drawable_timer) {}
        AppFeatureType.IMAGES -> FeatureItem(type, Res.string.images, UiRes.drawable.ui_drawable_image) {}
        AppFeatureType.VIDEOS -> FeatureItem(type, Res.string.videos, UiRes.drawable.ui_drawable_video) {}
        AppFeatureType.AUDIO -> FeatureItem(type, Res.string.audios, UiRes.drawable.ui_drawable_music) {}
        AppFeatureType.FILES -> FeatureItem(type, Res.string.files, UiRes.drawable.ui_drawable_folder) {}
        else -> FeatureItem(type, Res.string.tools, UiRes.drawable.ui_drawable_layout_grid) {}
    }
}
