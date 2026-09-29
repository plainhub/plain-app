package com.ismartcoding.plain.ui.page.home

import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.audio_lines as ui_drawable_audio_lines
import com.ismartcoding.plain.ui.resources.file_text as ui_drawable_file_text
import com.ismartcoding.plain.ui.resources.folder as ui_drawable_folder
import com.ismartcoding.plain.ui.resources.image as ui_drawable_image
import com.ismartcoding.plain.ui.resources.layout_grid as ui_drawable_layout_grid
import com.ismartcoding.plain.ui.resources.music as ui_drawable_music
import com.ismartcoding.plain.ui.resources.notebook_pen as ui_drawable_notebook_pen
import com.ismartcoding.plain.ui.resources.rss as ui_drawable_rss
import com.ismartcoding.plain.ui.resources.square_pen as ui_drawable_square_pen
import com.ismartcoding.plain.ui.resources.timer as ui_drawable_timer
import com.ismartcoding.plain.ui.resources.video as ui_drawable_video
import com.ismartcoding.plain.i18n.folder
import com.ismartcoding.plain.i18n.video
import com.ismartcoding.plain.i18n.image

fun FeatureItem.Companion.getList(navController: NavHostController): List<FeatureItem> {
    val list = mutableListOf(
        FeatureItem(AppFeatureType.IMAGES, Res.string.images, UiRes.drawable.ui_drawable_image) {
            navController.navigate(Routing.Images)
        },
        FeatureItem(AppFeatureType.FILES, Res.string.files, UiRes.drawable.ui_drawable_folder) {
            navController.navigate(Routing.Files())
        },
        FeatureItem(AppFeatureType.DOCS, Res.string.docs, UiRes.drawable.ui_drawable_file_text) {
            navController.navigate(Routing.Docs)
        },
    )

    if (AppFeatureType.APPS.has()) {
        list.add(FeatureItem(AppFeatureType.APPS, Res.string.apps, UiRes.drawable.ui_drawable_layout_grid) {
            navController.navigate(Routing.Apps)
        })
    }

    list.addAll(
        listOf(
            FeatureItem(AppFeatureType.NOTES, Res.string.notes, UiRes.drawable.ui_drawable_notebook_pen) {
                navController.navigate(Routing.Notes)
            },
            FeatureItem(AppFeatureType.FEEDS, Res.string.feeds, UiRes.drawable.ui_drawable_rss) {
                navController.navigate(Routing.FeedEntries(""))
            },
            FeatureItem(AppFeatureType.AUDIO, Res.string.audios, UiRes.drawable.ui_drawable_music) {
                navController.navigate(Routing.Audio)
            },
            FeatureItem(AppFeatureType.VIDEOS, Res.string.videos, UiRes.drawable.ui_drawable_video) {
                navController.navigate(Routing.Videos)
            },
            FeatureItem(AppFeatureType.SOUND_METER, Res.string.sound_meter, UiRes.drawable.ui_drawable_audio_lines) {
                navController.navigate(Routing.SoundMeter)
            },
            FeatureItem(AppFeatureType.POMODORO_TIMER, Res.string.pomodoro_timer, UiRes.drawable.ui_drawable_timer) {
                navController.navigate(Routing.PomodoroTimer)
            },
        )
    )

    if (AppFeatureType.IMAGE_EDITOR.has()) {
        list.add(
            FeatureItem(AppFeatureType.IMAGE_EDITOR, Res.string.image_editor, UiRes.drawable.ui_drawable_square_pen) {
                navController.navigate(Routing.ImageEditor)
            },
        )
    }

    return list
}
