package com.ismartcoding.plain.ui.page.root.home

import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.ui.page.root.components.RootPageType

data class FeatureItem(
    val type: AppFeatureType,
    val titleRes: Int,
    val iconRes: Int,
    val click: () -> Unit,
) {
    companion object {

        fun getList(onNavigate: (RootPageType) -> Unit): List<FeatureItem> {
            val list = mutableListOf(
                FeatureItem(AppFeatureType.FILES, R.string.files, R.drawable.folder) {
                    onNavigate(RootPageType.FILES)
                },
                FeatureItem(AppFeatureType.DOCS, R.string.docs, R.drawable.file_text) {
                    onNavigate(RootPageType.DOCS)
                }
            )

            if (AppFeatureType.APPS.has()) {
                list.add(FeatureItem(AppFeatureType.APPS, R.string.apps, R.drawable.layout_grid) {
                    onNavigate(RootPageType.APPS)
                })
            }

            list.addAll(
                listOf(
                    FeatureItem(AppFeatureType.NOTES, R.string.notes, R.drawable.notebook_pen) {
                        onNavigate(RootPageType.NOTES)
                    },
                    FeatureItem(AppFeatureType.FEEDS, R.string.feeds, R.drawable.rss) {
                        onNavigate(RootPageType.FEEDS)
                    },
                    FeatureItem(AppFeatureType.SOUND_METER, R.string.sound_meter, R.drawable.audio_lines) {
                        onNavigate(RootPageType.SOUND_METER)
                    },
                    FeatureItem(AppFeatureType.POMODORO_TIMER, R.string.pomodoro_timer, R.drawable.timer) {
                        onNavigate(RootPageType.POMODORO)
                    },
                )
            )

            return list
        }

    }
}
