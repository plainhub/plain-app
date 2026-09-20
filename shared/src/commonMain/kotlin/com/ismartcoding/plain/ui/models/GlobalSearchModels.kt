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

/** Content domains of the global search, in the fixed section order of the results page. */
enum class GlobalSearchDomain(
    val labelRes: StringResource,
    val iconRes: DrawableResource,
) {
    NOTES(Res.string.notes, Res.drawable.notebook_pen),
    AUDIO(Res.string.audios, Res.drawable.music),
    IMAGES(Res.string.images, Res.drawable.image),
    VIDEOS(Res.string.videos, Res.drawable.video),
    DOCS(Res.string.docs, Res.drawable.file_text),
    FILES(Res.string.files, Res.drawable.folder),
    FEEDS(Res.string.feeds, Res.drawable.rss),
    CHAT(Res.string.chat, Res.drawable.message_circle),
    APPS(Res.string.apps, Res.drawable.layout_grid);
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
