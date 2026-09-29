package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.i18n.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.data.DDoc
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.platform.DPackageInfo
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.file_text as ui_drawable_file_text
import com.ismartcoding.plain.ui.resources.folder as ui_drawable_folder
import com.ismartcoding.plain.ui.resources.image as ui_drawable_image
import com.ismartcoding.plain.ui.resources.layout_grid as ui_drawable_layout_grid
import com.ismartcoding.plain.ui.resources.message_circle as ui_drawable_message_circle
import com.ismartcoding.plain.ui.resources.music as ui_drawable_music
import com.ismartcoding.plain.ui.resources.notebook_pen as ui_drawable_notebook_pen
import com.ismartcoding.plain.ui.resources.rss as ui_drawable_rss
import com.ismartcoding.plain.ui.resources.video as ui_drawable_video
import com.ismartcoding.plain.i18n.folder
import com.ismartcoding.plain.i18n.video
import com.ismartcoding.plain.i18n.image

/** Content domains of the global search, in the fixed section order of the results page. */
enum class GlobalSearchDomain(
    val labelRes: StringResource,
    val iconRes: DrawableResource,
) {
    NOTES(Res.string.notes, UiRes.drawable.ui_drawable_notebook_pen),
    AUDIO(Res.string.audios, UiRes.drawable.ui_drawable_music),
    IMAGES(Res.string.images, UiRes.drawable.ui_drawable_image),
    VIDEOS(Res.string.videos, UiRes.drawable.ui_drawable_video),
    DOCS(Res.string.docs, UiRes.drawable.ui_drawable_file_text),
    FILES(Res.string.files, UiRes.drawable.ui_drawable_folder),
    FEEDS(Res.string.feeds, UiRes.drawable.ui_drawable_rss),
    CHAT(Res.string.chat, UiRes.drawable.ui_drawable_message_circle),
    APPS(Res.string.apps, UiRes.drawable.ui_drawable_layout_grid);
}

fun globalSearchDomains(): List<GlobalSearchDomain> =
    if (AppFeatureType.APPS.has()) GlobalSearchDomain.entries.toList()
    else GlobalSearchDomain.entries.filter { it != GlobalSearchDomain.APPS }

/** What tapping a search result does. */
sealed interface GlobalSearchAction {
    data class Navigate(val route: Any) : GlobalSearchAction
    data class PlayAudio(val audio: DAudio) : GlobalSearchAction
}

/** Source model for domains whose rows render with the source page's own list item component. */
sealed interface GlobalSearchSource {
    data class Note(val note: DNote) : GlobalSearchSource
    data class Audio(val audio: DAudio) : GlobalSearchSource
    data class Image(val image: DImage) : GlobalSearchSource
    data class Video(val video: DVideo) : GlobalSearchSource
    data class Doc(val doc: DDoc) : GlobalSearchSource
    data class File(val file: DFile) : GlobalSearchSource
    data class Feed(val entry: DFeedEntry) : GlobalSearchSource
    data class App(val info: DPackageInfo) : GlobalSearchSource
}

data class GlobalSearchHit(
    val key: String,
    val title: String,
    val subtitle: String = "",
    val snippet: String = "",
    val iconRes: DrawableResource? = null,
    val roundThumb: Boolean = false,
    val source: GlobalSearchSource? = null,
    val action: GlobalSearchAction,
)

/** Per-domain counters and loaded rows for one search. */
class GlobalSearchDomainState {
    val total = mutableIntStateOf(0)
    val loaded = mutableIntStateOf(0)
    val hits = mutableStateOf<List<GlobalSearchHit>>(emptyList())
    val loading = mutableStateOf(false)
}

/** Window of [this] around the first occurrence of [q] for a two-line snippet. */
fun String.snippetAround(q: String, radius: Int = 40): String {
    val text = trim()
    if (text.isEmpty()) return ""
    val ql = q.lowercase()
    if (ql.isEmpty()) return text.take(radius * 2)
    val idx = text.lowercase().indexOf(ql)
    if (idx < 0) return text.take(radius * 2)
    val start = (idx - radius).coerceAtLeast(0)
    val end = (idx + ql.length + radius).coerceAtMost(text.length)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < text.length) "…" else ""
    return prefix + text.substring(start, end) + suffix
}
