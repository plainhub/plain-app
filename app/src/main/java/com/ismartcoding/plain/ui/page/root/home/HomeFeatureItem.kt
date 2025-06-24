package com.ismartcoding.plain.ui.page.root.home

import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.nav.navigateFiles

data class FeatureItem(
    val type: AppFeatureType,
    val titleRes: Int,
    val iconRes: Int,
    val click: () -> Unit,
) {
    companion object {

        fun getList(navController: NavHostController): List<FeatureItem> {
            val list = mutableListOf(
                FeatureItem(AppFeatureType.FILES, R.string.files, R.drawable.folder) {
                    navController.navigateFiles()
                },
                FeatureItem(AppFeatureType.DOCS, R.string.docs, R.drawable.file_text) {
                    navController.navigate(Routing.Docs)
                }
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
                    FeatureItem(AppFeatureType.FEEDS, R.string.feeds, R.drawable.rss, {
                        navController.navigate(Routing.FeedEntries(""))
                    }),
                    FeatureItem(AppFeatureType.SOUND_METER, R.string.sound_meter, R.drawable.audio_lines) {
                        navController.navigate(Routing.SoundMeter)
                    },
                    FeatureItem(AppFeatureType.POMODORO_TIMER, R.string.pomodoro_timer, R.drawable.timer) {
                        navController.navigate(Routing.PomodoroTimer)
                    },
                )
            )

            return list
        }

    }
}