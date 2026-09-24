package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.data.DDoc
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.db.getMessagePreview
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.getIcon
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.formatDurationMs
import com.ismartcoding.plain.platform.DPackageInfo
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.countFiles
import com.ismartcoding.plain.platform.countMedia
import com.ismartcoding.plain.platform.countPackages
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.platform.searchFiles
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.platform.searchPackages
import com.ismartcoding.plain.ui.nav.Routing
import org.jetbrains.compose.resources.DrawableResource

/**
 * Data-source layer of the global search: how each domain is counted,
 * queried and re-synced, and how its source models map to result hits.
 * The [GlobalSearchViewModel] owns state and orchestration only.
 */

internal suspend fun GlobalSearchViewModel.countDomain(d: GlobalSearchDomain, q: String): Int =
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

internal suspend fun GlobalSearchViewModel.queryDomain(
    d: GlobalSearchDomain,
    q: String,
    limit: Int,
    offset: Int,
): List<GlobalSearchHit> =
    when (d) {
        GlobalSearchDomain.NOTES ->
            NoteHelper.search("$q trash:false", limit, offset).map { it.toHit(q) }
        GlobalSearchDomain.AUDIO ->
            searchMedia(DataType.AUDIO, "$q trash:false", limit, offset, FileSortBy.DATE_DESC)
                .filterIsInstance<DAudio>().map { it.toHit() }
        GlobalSearchDomain.IMAGES ->
            searchMedia(DataType.IMAGE, "$q trash:false", limit, offset, FileSortBy.DATE_DESC)
                .filterIsInstance<DImage>().map { it.toHit() }
        GlobalSearchDomain.VIDEOS ->
            searchMedia(DataType.VIDEO, "$q trash:false", limit, offset, FileSortBy.DATE_DESC)
                .filterIsInstance<DVideo>().map { it.toHit() }
        GlobalSearchDomain.DOCS ->
            searchMedia(DataType.DOC, "$q trash:false", limit, offset, FileSortBy.DATE_DESC)
                .filterIsInstance<DDoc>().map { it.toHit() }
        GlobalSearchDomain.FILES ->
            searchFiles(q, limit, offset, FileSortBy.DATE_DESC).map { it.toHit() }
        GlobalSearchDomain.FEEDS ->
            FeedEntryHelper.search(q, limit, offset).map { it.toHit(q) }
        GlobalSearchDomain.CHAT ->
            ChatDbHelper.searchAsync(q, limit, offset).map { it.toHit(q) }
        GlobalSearchDomain.APPS ->
            searchPackages(q, limit, offset, FileSortBy.DATE_DESC).filter { it.name.isNotBlank() }.map { it.toHit() }
    }

/**
 * Re-fetches the currently loaded hits of an editable domain by their source
 * ids; entries that no longer exist fall out. Returns null for domains
 * without an editable source (files, chat, apps).
 */
internal suspend fun GlobalSearchViewModel.reloadDomainHits(
    d: GlobalSearchDomain,
    hits: List<GlobalSearchHit>,
    q: String,
): List<GlobalSearchHit>? =
    when (d) {
        GlobalSearchDomain.NOTES -> hits.mapNotNull { hit ->
            val src = hit.source as? GlobalSearchSource.Note ?: return@mapNotNull hit
            NoteHelper.getById(src.note.id)?.toHit(q)
        }
        GlobalSearchDomain.FEEDS -> hits.mapNotNull { hit ->
            val src = hit.source as? GlobalSearchSource.Feed ?: return@mapNotNull hit
            FeedEntryHelper.getAsync(src.entry.id)?.toHit(q)
        }
        GlobalSearchDomain.AUDIO -> reloadMediaHits<DAudio>(hits, DataType.AUDIO) { it.toHit() }
        GlobalSearchDomain.IMAGES -> reloadMediaHits<DImage>(hits, DataType.IMAGE) { it.toHit() }
        GlobalSearchDomain.VIDEOS -> reloadMediaHits<DVideo>(hits, DataType.VIDEO) { it.toHit() }
        GlobalSearchDomain.DOCS -> reloadMediaHits<DDoc>(hits, DataType.DOC) { it.toHit() }
        else -> null
    }

private suspend inline fun <reified T : Any> reloadMediaHits(
    hits: List<GlobalSearchHit>,
    dataType: DataType,
    crossinline toHit: (T) -> GlobalSearchHit,
): List<GlobalSearchHit>? {
    val ids = hits.mapNotNull { hit ->
        when (val src = hit.source) {
            is GlobalSearchSource.Audio -> if (dataType == DataType.AUDIO) src.audio.id else null
            is GlobalSearchSource.Image -> if (dataType == DataType.IMAGE) src.image.id else null
            is GlobalSearchSource.Video -> if (dataType == DataType.VIDEO) src.video.id else null
            is GlobalSearchSource.Doc -> if (dataType == DataType.DOC) src.doc.id else null
            else -> null
        }
    }
    if (ids.isEmpty()) return null
    return searchMedia(dataType, "ids:${ids.joinToString(",")} trash:false", ids.size, 0, FileSortBy.DATE_DESC)
        .filterIsInstance<T>()
        .map { toHit(it) }
}

private fun DNote.toHit(q: String): GlobalSearchHit = GlobalSearchHit(
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

private fun DImage.toHit(): GlobalSearchHit = GlobalSearchHit(
    key = "image_$id",
    title = title,
    subtitle = (takenAt ?: createdAt).formatDateTime(),
    source = GlobalSearchSource.Image(this),
    action = GlobalSearchAction.Navigate(Routing.Images),
)

private fun DVideo.toHit(): GlobalSearchHit = GlobalSearchHit(
    key = "video_$id",
    title = title,
    subtitle = durationMs.formatDurationMs() + " · " + (takenAt ?: createdAt).formatDateTime(),
    source = GlobalSearchSource.Video(this),
    action = GlobalSearchAction.Navigate(Routing.Videos),
)

private fun DDoc.toHit(): GlobalSearchHit = GlobalSearchHit(
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

private fun DChat.toHit(q: String): GlobalSearchHit {
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
    val thumbRes: DrawableResource = if (isChannel) {
        Res.drawable.hash
    } else {
        val other = if (fromId == "me" || fromId == "local") toId else fromId
        if (other == "local") Res.drawable.bot
        else PeerCacher.getPeer(other)?.deviceType?.getIcon() ?: Res.drawable.devices
    }
    return GlobalSearchHit(
        key = "chat_$id",
        title = senderName,
        snippet = getMessagePreview().snippetAround(q),
        subtitle = conversation,
        iconRes = thumbRes,
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
