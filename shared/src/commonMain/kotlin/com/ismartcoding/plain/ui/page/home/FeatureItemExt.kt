package com.ismartcoding.plain.ui.page.home

import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.nav.Routing

fun FeatureItem.Companion.getList(navController: NavHostController): List<FeatureItem> {
    val list = mutableListOf(
        FeatureItem(AppFeatureType.IMAGES, Res.string.images, Res.drawable.image) {
            navController.navigate(Routing.Images)
        },
        FeatureItem(AppFeatureType.FILES, Res.string.files, Res.drawable.folder) {
            navController.navigate(Routing.Files())
        },
        FeatureItem(AppFeatureType.DOCS, Res.string.docs, Res.drawable.file_text) {
            navController.navigate(Routing.Docs)
        },
    )

    if (AppFeatureType.APPS.has()) {
        list.add(FeatureItem(AppFeatureType.APPS, Res.string.apps, Res.drawable.layout_grid) {
            navController.navigate(Routing.Apps)
        })
    }

    list.addAll(
        listOf(
            FeatureItem(AppFeatureType.NOTES, Res.string.notes, Res.drawable.notebook_pen) {
                navController.navigate(Routing.Notes)
            },
            FeatureItem(AppFeatureType.FEEDS, Res.string.feeds, Res.drawable.rss) {
                navController.navigate(Routing.Feeds)
            },
            FeatureItem(AppFeatureType.CHAT, Res.string.chat, Res.drawable.message_circle) {
                navController.navigate(Routing.ChatList)
            },
            FeatureItem(AppFeatureType.AUDIO, Res.string.audios, Res.drawable.music) {
                navController.navigate(Routing.Audio)
            },
            FeatureItem(AppFeatureType.VIDEOS, Res.string.videos, Res.drawable.video) {
                navController.navigate(Routing.Videos)
            },
            FeatureItem(AppFeatureType.SOUND_METER, Res.string.sound_meter, Res.drawable.audio_lines) {
                navController.navigate(Routing.SoundMeter)
            },
            FeatureItem(AppFeatureType.POMODORO_TIMER, Res.string.pomodoro_timer, Res.drawable.timer) {
                navController.navigate(Routing.PomodoroTimer)
            },
            FeatureItem(AppFeatureType.DLNA_RECEIVER, Res.string.dlna_receiver, Res.drawable.cast) {
                navController.navigate(Routing.DlnaReceiver)
            },
        )
    )

    return list
}
