package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.i18n.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.db.getMessagePreview
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.getIcon
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.formatDuration
import com.ismartcoding.plain.platform.DPackageInfo
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.countFiles
import com.ismartcoding.plain.platform.countMedia
import com.ismartcoding.plain.platform.countPackages
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.platform.getMediaItemUriString
import com.ismartcoding.plain.platform.searchFiles
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.platform.searchPackages
import com.ismartcoding.plain.preferences.RecentSearchesPreference
import com.ismartcoding.plain.ui.nav.Routing
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

/** Leading visual of a hit row rendered with the generic row (chat, images, videos). */
sealed interface GlobalSearchThumbModel {
    data class Icon(val res: DrawableResource) : GlobalSearchThumbModel
    data class Media(val uri: String) : GlobalSearchThumbModel
}

/** Source model for domains whose rows render with the source page's own list item component. */
sealed interface GlobalSearchSource {
    data class Note(val note: com.ismartcoding.plain.db.DNote) : GlobalSearchSource
    data class Audio(val audio: DAudio) : GlobalSearchSource
    data class Image(val image: com.ismartcoding.plain.data.DImage) : GlobalSearchSource
    data class Video(val video: com.ismartcoding.plain.data.DVideo) : GlobalSearchSource
    data class Doc(val doc: com.ismartcoding.plain.data.DDoc) : GlobalSearchSource
    data class File(val file: DFile) : GlobalSearchSource
    data class Feed(val entry: DFeedEntry) : GlobalSearchSource
    data class App(val info: DPackageInfo) : GlobalSearchSource
}

class GlobalSearchHit(
    val key: String,
    val title: String,
    val subtitle: String = "",
    val snippet: String = "",
    val thumb: GlobalSearchThumbModel? = null,
    val roundThumb: Boolean = false,
    val source: GlobalSearchSource? = null,
    val action: GlobalSearchAction,
)

class GlobalSearchDomainState {
    val total = mutableIntStateOf(0)
    val loaded = mutableIntStateOf(0)
    val hits = mutableStateOf<List<GlobalSearchHit>>(emptyList())
    val loading = mutableStateOf(false)
}

class GlobalSearchViewModel : ViewModel() {
    var queryText = mutableStateOf("")
    /** null = search every domain. */
    var domain = mutableStateOf<GlobalSearchDomain?>(null)
    /** false = live suggestions while typing, true = submitted results. */
    var submitted = mutableStateOf(false)
    var searching = mutableStateOf(false)
    /** True once the current query finished with zero hits everywhere (drives the empty state). */
    var empty = mutableStateOf(false)
    val domainStates = mutableStateMapOf<GlobalSearchDomain, GlobalSearchDomainState>()
    var recentQueries = mutableStateOf<List<String>>(emptyList())

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            recentQueries.value = RecentSearchesPreference.getValueAsync()
        }
    }

    fun onQueryChange(q: String) {
        if (queryText.value == q) return
        queryText.value = q
        submitted.value = false
        if (q.isBlank()) resetResults()
    }

    fun setDomain(d: GlobalSearchDomain?) {
        if (domain.value == d) return
        domain.value = d
        if (queryText.value.isNotBlank()) {
            search()
        }
    }

    /** Switches the scope to a single domain with its full result list (from a suggestion section). */
    fun viewAllOfType(d: GlobalSearchDomain) {
        domain.value = d
        submitted.value = true
        search()
    }

    fun submit() {
        val q = queryText.value.trim()
        if (q.isEmpty()) return
        submitted.value = true
        recordRecent(q)
        search()
    }

    fun searchFromRecent(q: String) {
        queryText.value = q
        submitted.value = true
        recordRecent(q)
        search()
    }

    fun removeRecent(q: String) {
        viewModelScope.launch {
            recentQueries.value = RecentSearchesPreference.removeAsync(q)
        }
    }

    fun clearRecent() {
        viewModelScope.launch {
            RecentSearchesPreference.clearAsync()
            recentQueries.value = emptyList()
        }
    }

    private fun recordRecent(q: String) {
        viewModelScope.launch {
            recentQueries.value = RecentSearchesPreference.recordAsync(q)
        }
    }

    fun loadMore(d: GlobalSearchDomain) {
        val q = queryText.value.trim()
        if (q.isEmpty()) return
        val state = domainStates[d] ?: return
        if (state.loading.value || state.loaded.value >= state.total.value) return
        viewModelScope.launch {
            state.loading.value = true
            try {
                val hits = queryDomain(d, q, PAGE_SIZE, state.loaded.value)
                state.hits.value += hits
                state.loaded.value += hits.size
            } finally {
                state.loading.value = false
            }
        }
    }

    private fun resetResults() {
        searchJob?.cancel()
        searchJob = null
        domainStates.clear()
        searching.value = false
        empty.value = false
    }

    fun search() {
        val q = queryText.value.trim()
        if (q.isEmpty()) {
            resetResults()
            return
        }
        searchJob?.cancel()
        val scope = domain.value
        val targets = scope?.let { listOf(it) } ?: globalSearchDomains()
        val fresh = targets.associateWith { GlobalSearchDomainState() }
        domainStates.clear()
        targets.forEach { domainStates[it] = fresh.getValue(it) }
        searching.value = true
        empty.value = false
        searchJob = viewModelScope.launch {
            try {
                coroutineScope {
                    targets.forEach { d ->
                        async {
                            val state = fresh.getValue(d)
                            state.loading.value = true
                            try {
                                val total = countDomain(d, q)
                                state.total.value = total
                                if (total > 0) {
                                    val hits = queryDomain(d, q, PAGE_SIZE, 0)
                                    state.hits.value = hits
                                    state.loaded.value = hits.size
                                }
                            } finally {
                                state.loading.value = false
                            }
                        }
                    }
                }
                empty.value = fresh.values.all { it.total.intValue == 0 }
            } finally {
                searching.value = false
            }
        }
    }

    private suspend fun countDomain(d: GlobalSearchDomain, q: String): Int =
        when (d) {
            GlobalSearchDomain.NOTES -> NoteHelper.count("$q trash:false")
            GlobalSearchDomain.AUDIO -> countMedia(DataType.AUDIO, "$q trash:false")
            GlobalSearchDomain.IMAGES -> countMedia(DataType.IMAGE, "$q trash:false")
            GlobalSearchDomain.VIDEOS -> countMedia(DataType.VIDEO, "$q trash:false")
            GlobalSearchDomain.DOCS -> countMedia(DataType.DOC, "$q trash:false")
            GlobalSearchDomain.FILES -> countFiles(q)
            GlobalSearchDomain.FEEDS -> FeedEntryHelper.count(q)
            GlobalSearchDomain.CHAT -> ChatDbHelper.countAsync(q)
            GlobalSearchDomain.APPS -> countPackages(q)
        }

    private suspend fun queryDomain(d: GlobalSearchDomain, q: String, limit: Int, offset: Int): List<GlobalSearchHit> =
        when (d) {
            GlobalSearchDomain.NOTES ->
                NoteHelper.search("$q trash:false", limit, offset).map { it.toHit(q) }
            GlobalSearchDomain.AUDIO ->
                searchMedia(DataType.AUDIO, "$q trash:false", limit, offset, FileSortBy.DATE_DESC)
                    .filterIsInstance<DAudio>().map { it.toHit() }
            GlobalSearchDomain.IMAGES ->
                searchMedia(DataType.IMAGE, "$q trash:false", limit, offset, FileSortBy.DATE_DESC)
                    .filterIsInstance<com.ismartcoding.plain.data.DImage>().map { it.toHit() }
            GlobalSearchDomain.VIDEOS ->
                searchMedia(DataType.VIDEO, "$q trash:false", limit, offset, FileSortBy.DATE_DESC)
                    .filterIsInstance<com.ismartcoding.plain.data.DVideo>().map { it.toHit() }
            GlobalSearchDomain.DOCS ->
                searchMedia(DataType.DOC, "$q trash:false", limit, offset, FileSortBy.DATE_DESC)
                    .filterIsInstance<com.ismartcoding.plain.data.DDoc>().map { it.toHit() }
            GlobalSearchDomain.FILES ->
                searchFiles(q, limit, offset, FileSortBy.DATE_DESC).map { it.toHit() }
            GlobalSearchDomain.FEEDS ->
                FeedEntryHelper.search(q, limit, offset).map { it.toHit(q) }
            GlobalSearchDomain.CHAT ->
                ChatDbHelper.searchAsync(q, limit, offset).map { it.toHit(q) }
            GlobalSearchDomain.APPS ->
                searchPackages(q, limit, offset, FileSortBy.DATE_DESC).filter { it.name.isNotBlank() }.map { it.toHit() }
        }

    private fun com.ismartcoding.plain.db.DNote.toHit(q: String): GlobalSearchHit = GlobalSearchHit(
        key = "note_$id",
        title = title,
        snippet = content.snippetAround(q),
        source = GlobalSearchSource.Note(this),
        action = GlobalSearchAction.Navigate(Routing.NoteDetail(id)),
    )

    private fun DAudio.toHit(): GlobalSearchHit = GlobalSearchHit(
        key = "audio_$id",
        title = title,
        subtitle = artist,
        source = GlobalSearchSource.Audio(this),
        action = GlobalSearchAction.PlayAudio(this),
    )

    private fun com.ismartcoding.plain.data.DImage.toHit(): GlobalSearchHit = GlobalSearchHit(
        key = "image_$id",
        title = title,
        subtitle = (takenAt ?: createdAt).formatDateTime(),
        thumb = GlobalSearchThumbModel.Media(getMediaItemUriString(DataType.IMAGE, id)),
        source = GlobalSearchSource.Image(this),
        action = GlobalSearchAction.Navigate(Routing.Images),
    )

    private fun com.ismartcoding.plain.data.DVideo.toHit(): GlobalSearchHit = GlobalSearchHit(
        key = "video_$id",
        title = title,
        subtitle = duration.formatDuration() + " · " + (takenAt ?: createdAt).formatDateTime(),
        thumb = GlobalSearchThumbModel.Media(getMediaItemUriString(DataType.VIDEO, id)),
        source = GlobalSearchSource.Video(this),
        action = GlobalSearchAction.Navigate(Routing.Videos),
    )

    private fun com.ismartcoding.plain.data.DDoc.toHit(): GlobalSearchHit = GlobalSearchHit(
        key = "doc_$id",
        title = title,
        subtitle = size.formatBytes(),
        source = GlobalSearchSource.Doc(this),
        action = GlobalSearchAction.Navigate(Routing.Docs),
    )

    private fun DFile.toHit(): GlobalSearchHit = GlobalSearchHit(
        key = "file_$path",
        title = name,
        subtitle = size.formatBytes(),
        source = GlobalSearchSource.File(this),
        action = GlobalSearchAction.Navigate(Routing.Files(path.substringBeforeLast('/'))),
    )

    private fun DFeedEntry.toHit(q: String): GlobalSearchHit = GlobalSearchHit(
        key = "feed_$id",
        title = title,
        snippet = description.snippetAround(q),
        source = GlobalSearchSource.Feed(this),
        action = GlobalSearchAction.Navigate(Routing.FeedEntry(id)),
    )

    private fun com.ismartcoding.plain.db.DChat.toHit(q: String): GlobalSearchHit {
        val isChannel = channelId.isNotEmpty()
        val conversation: String
        val chatRoute: String
        val senderName: String
        if (isChannel) {
            conversation = ChannelCacher.getChannel(channelId)?.name ?: channelId
            chatRoute = "channel:$channelId"
            senderName = if (fromId == "me") LocaleHelper.getString(Res.string.me)
            else PeerCacher.getPeer(fromId)?.name ?: fromId
        } else {
            val other = if (fromId == "me" || fromId == "local") toId else fromId
            conversation = if (other == "local") LocaleHelper.getString(Res.string.local_chat)
            else PeerCacher.getPeer(other)?.name ?: other
            chatRoute = "peer:$other"
            senderName = if (fromId == "me") LocaleHelper.getString(Res.string.me) else conversation
        }
        val thumb = if (isChannel) {
            GlobalSearchThumbModel.Icon(Res.drawable.hash)
        } else {
            val other = if (fromId == "me" || fromId == "local") toId else fromId
            GlobalSearchThumbModel.Icon(
                if (other == "local") Res.drawable.bot
                else PeerCacher.getPeer(other)?.deviceType?.getIcon() ?: Res.drawable.devices,
            )
        }
        return GlobalSearchHit(
            key = "chat_$id",
            title = senderName,
            snippet = getMessagePreview().snippetAround(q),
            subtitle = conversation,
            thumb = thumb,
            roundThumb = true,
            action = GlobalSearchAction.Navigate(Routing.Chat(chatRoute)),
        )
    }

    private fun DPackageInfo.toHit(): GlobalSearchHit = GlobalSearchHit(
        key = "app_$id",
        title = name,
        subtitle = id,
        source = GlobalSearchSource.App(this),
        action = GlobalSearchAction.Navigate(Routing.AppDetails(id)),
    )

    companion object {
        /** Rows fetched per domain page and shown per domain section in results mode. */
        const val PAGE_SIZE = 8

        /** Rows shown per domain while typing (suggestion mode); snippet domains show fewer. */
        const val SUGGEST_ROWS = 3
        const val SUGGEST_ROWS_WITH_SNIPPET = 2
    }
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
