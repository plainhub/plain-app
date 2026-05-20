package com.ismartcoding.plain.ui.page.home

import org.jetbrains.compose.resources.DrawableResource
import com.ismartcoding.plain.i18n.*
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.ui.nav.Routing

data class FeatureItem(
    val type: AppFeatureType,
    val titleRes: Int,
    val iconRes: DrawableResource,
    val click: () -> Unit,
) {
    companion object {

        fun getList(navController: NavHostController): List<FeatureItem> {
            val list = mutableListOf(
                FeatureItem(AppFeatureType.IMAGES, R.string.images, Res.drawable.image) {
                    navController.navigate(Routing.Images)
                },
                FeatureItem(AppFeatureType.FILES, R.string.files, Res.drawable.folder) {
                    navController.navigate(Routing.Files())
                },
                FeatureItem(AppFeatureType.DOCS, R.string.docs, Res.drawable.file_text) {
                    navController.navigate(Routing.Docs)
                },
            )

            if (AppFeatureType.APPS.has()) {
                list.add(FeatureItem(AppFeatureType.APPS, R.string.apps, Res.drawable.layout_grid) {
                    navController.navigate(Routing.Apps)
                })
            }

            list.addAll(
                listOf(
                    FeatureItem(AppFeatureType.NOTES, R.string.notes, Res.drawable.notebook_pen) {
                        navController.navigate(Routing.Notes)
                    },
                    FeatureItem(AppFeatureType.FEEDS, R.string.feeds, Res.drawable.rss) {
                        navController.navigate(Routing.Feeds)
                    },
                    FeatureItem(AppFeatureType.CHAT, R.string.chat, Res.drawable.message_circle) {
                        navController.navigate(Routing.ChatList)
                    },
                    FeatureItem(AppFeatureType.AUDIO, R.string.audios, Res.drawable.music) {
                        navController.navigate(Routing.Audio)
                    },
                    FeatureItem(AppFeatureType.VIDEOS, R.string.videos, Res.drawable.video) {
                        navController.navigate(Routing.Videos)
                    },
                    FeatureItem(AppFeatureType.SOUND_METER, R.string.sound_meter, Res.drawable.audio_lines) {
                        navController.navigate(Routing.SoundMeter)
                    },
                    FeatureItem(AppFeatureType.POMODORO_TIMER, R.string.pomodoro_timer, Res.drawable.timer) {
                        navController.navigate(Routing.PomodoroTimer)
                    },
                    FeatureItem(AppFeatureType.DLNA_RECEIVER, R.string.dlna_receiver, Res.drawable.cast) {
                        navController.navigate(Routing.DlnaReceiver)
                    },
                )
            )

            return list
        }

    }
}
