package com.ismartcoding.plain.ui.page.home

import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.ui.nav.Routing

data class FeatureItem(
    val type: AppFeatureType,
    val titleRes: Int,
    val iconRes: Int,
    val click: () -> Unit,
) {
    companion object {

        fun getList(navController: NavHostController): List<FeatureItem> {
            val list = mutableListOf(
                FeatureItem(AppFeatureType.IMAGES, R.string.images, R.drawable.image) {
                    navController.navigate(Routing.Images)
                },
                FeatureItem(AppFeatureType.FILES, R.string.files, R.drawable.folder) {
                    navController.navigate(Routing.Files())
                },
                FeatureItem(AppFeatureType.DOCS, R.string.docs, R.drawable.file_text) {
                    navController.navigate(Routing.Docs)
                },
            )

            if (AppFeatureType.APPS.has()) {
                list.add(FeatureItem(AppFeatureType.APPS, R.string.apps, R.drawable.layout_grid) {
                    navController.navigate(Routing.Apps)
                })
            }

            list.addAll(
                listOf(
                    FeatureItem(AppFeatureType.NOTES, R.string.notes, R.drawable.notebook_pen) {
                        navController.navigate(Routing.Notes)
                    },
                    FeatureItem(AppFeatureType.FEEDS, R.string.feeds, R.drawable.rss) {
                        navController.navigate(Routing.Feeds)
                    },
                    FeatureItem(AppFeatureType.CHAT, R.string.chat, R.drawable.message_circle) {
                        navController.navigate(Routing.ChatList)
                    },
                    FeatureItem(AppFeatureType.AUDIO, R.string.audios, R.drawable.music) {
                        navController.navigate(Routing.Audio)
                    },
                    FeatureItem(AppFeatureType.VIDEOS, R.string.videos, R.drawable.video) {
                        navController.navigate(Routing.Videos)
                    },
                    FeatureItem(AppFeatureType.SOUND_METER, R.string.sound_meter, R.drawable.audio_lines) {
                        navController.navigate(Routing.SoundMeter)
                    },
                    FeatureItem(AppFeatureType.POMODORO_TIMER, R.string.pomodoro_timer, R.drawable.timer) {
                        navController.navigate(Routing.PomodoroTimer)
                    },
                    FeatureItem(AppFeatureType.DLNA_RECEIVER, R.string.dlna_receiver, R.drawable.cast) {
                        navController.navigate(Routing.DlnaReceiver)
                    },
                )
            )

            return list
        }

    }
}
