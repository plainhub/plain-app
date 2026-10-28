package com.ismartcoding.plain.ui.tools

import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.folder
import com.ismartcoding.plain.ui.resources.image
import com.ismartcoding.plain.ui.resources.video
import com.ismartcoding.plain.ui.resources.music
import com.ismartcoding.plain.ui.resources.file_text
import com.ismartcoding.plain.ui.resources.layout_grid
import com.ismartcoding.plain.ui.resources.notebook_pen
import com.ismartcoding.plain.ui.resources.rss

internal fun managedFeatureItems(capabilities: Collection<Capability>, onOpen: (AppFeatureType) -> Unit): List<FeatureItem> = listOf(
    FeatureItem(AppFeatureType.IMAGES, Res.string.images, UiRes.drawable.image) { onOpen(AppFeatureType.IMAGES) },
    FeatureItem(AppFeatureType.FILES, Res.string.files, UiRes.drawable.folder) { onOpen(AppFeatureType.FILES) },
    FeatureItem(AppFeatureType.DOCS, Res.string.docs, UiRes.drawable.file_text) { onOpen(AppFeatureType.DOCS) },
    FeatureItem(AppFeatureType.APPS, Res.string.apps, UiRes.drawable.layout_grid) { onOpen(AppFeatureType.APPS) },
    FeatureItem(AppFeatureType.NOTES, Res.string.notes, UiRes.drawable.notebook_pen) { onOpen(AppFeatureType.NOTES) },
    FeatureItem(AppFeatureType.FEEDS, Res.string.feeds, UiRes.drawable.rss) { onOpen(AppFeatureType.FEEDS) },
    FeatureItem(AppFeatureType.AUDIO, Res.string.audios, UiRes.drawable.music) { onOpen(AppFeatureType.AUDIO) },
    FeatureItem(AppFeatureType.VIDEOS, Res.string.videos, UiRes.drawable.video) { onOpen(AppFeatureType.VIDEOS) },
).filter { it.type.has(capabilities) }
