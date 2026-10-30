package com.ismartcoding.plain.ui.search

import com.ismartcoding.plain.models.File as FileEntry

import com.ismartcoding.plain.models.Audio as AudioTrack
import com.ismartcoding.plain.models.Doc as Document
import com.ismartcoding.plain.models.Video as MediaVideo
import com.ismartcoding.plain.models.Image as MediaImage
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.models.Audio
import com.ismartcoding.plain.models.Doc
import com.ismartcoding.plain.models.FeedEntry
import com.ismartcoding.plain.models.File
import com.ismartcoding.plain.models.Image
import com.ismartcoding.plain.models.Note
import com.ismartcoding.plain.models.Video
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.i18n.folder
import com.ismartcoding.plain.i18n.image
import com.ismartcoding.plain.i18n.video
import com.ismartcoding.plain.models.Package as AppPackage
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
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import com.ismartcoding.plain.models.Note as ModelNote

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

fun globalSearchDomains(): List<GlobalSearchDomain> = GlobalSearchDomain.entries.filter {
    it.supportedBy(com.ismartcoding.plain.rust.graphql.ContentSource.Local)
}

/** What tapping a search result does. */
sealed interface GlobalSearchAction {
    data class Navigate(val route: Any) : GlobalSearchAction
    data class PlayAudio(val audio: AudioTrack) : GlobalSearchAction
}

/** Source model for domains whose rows render with the source page's own list item component. */
sealed interface GlobalSearchSource {
    data class Note(val note: ModelNote) : GlobalSearchSource
    data class Audio(val audio: AudioTrack) : GlobalSearchSource
    data class Image(val image: MediaImage) : GlobalSearchSource
    data class Video(val video: MediaVideo) : GlobalSearchSource
    data class Doc(val doc: Document) : GlobalSearchSource
    data class File(val file: FileEntry) : GlobalSearchSource
    data class Feed(val entry: FeedEntry) : GlobalSearchSource
    data class App(val info: AppPackage) : GlobalSearchSource
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
    val device: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.rust.graphql.ContentSource.Local,
)

internal class GlobalSearchDeviceState(val device: com.ismartcoding.plain.rust.graphql.ContentSource) {
    val total = mutableIntStateOf(0)
    val loaded = mutableIntStateOf(0)
    val hits = mutableStateOf<List<GlobalSearchHit>>(emptyList())
    val loading = mutableStateOf(true)
    val error = mutableStateOf<String?>(null)
}

class GlobalSearchDomainState internal constructor(internal val devices: List<GlobalSearchDeviceState>) {
    val total = androidx.compose.runtime.derivedStateOf { devices.sumOf { it.total.intValue } }
    val loaded = androidx.compose.runtime.derivedStateOf { devices.sumOf { it.loaded.intValue } }
    val hits = androidx.compose.runtime.derivedStateOf { devices.flatMap { it.hits.value } }
    val loading = androidx.compose.runtime.derivedStateOf { devices.any { it.loading.value } }
    internal val hasErrors = androidx.compose.runtime.derivedStateOf { devices.any { it.error.value != null } }
}

internal data class GlobalSearchDeviceError(val name: String, val message: String)
internal data class GlobalSearchDevices(
    val devices: List<com.ismartcoding.plain.rust.graphql.ContentSource>,
    val errors: List<GlobalSearchDeviceError> = emptyList(),
)

internal fun GlobalSearchDomain.supportedBy(source: com.ismartcoding.plain.rust.graphql.ContentSource): Boolean {
    val capability = when (this) {
        GlobalSearchDomain.NOTES -> com.ismartcoding.plain.platform.system.Capability.NOTES
        GlobalSearchDomain.AUDIO -> com.ismartcoding.plain.platform.system.Capability.AUDIO_LIBRARY
        GlobalSearchDomain.IMAGES -> com.ismartcoding.plain.platform.system.Capability.IMAGE_LIBRARY
        GlobalSearchDomain.VIDEOS -> com.ismartcoding.plain.platform.system.Capability.VIDEO_LIBRARY
        GlobalSearchDomain.DOCS -> com.ismartcoding.plain.platform.system.Capability.DOCUMENT_LIBRARY
        GlobalSearchDomain.FILES -> com.ismartcoding.plain.platform.system.Capability.FILE_BROWSER
        GlobalSearchDomain.FEEDS -> com.ismartcoding.plain.platform.system.Capability.FEEDS
        GlobalSearchDomain.CHAT -> return true
        GlobalSearchDomain.APPS -> com.ismartcoding.plain.platform.system.Capability.PACKAGES
    }
    return capability in source.capabilities
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
