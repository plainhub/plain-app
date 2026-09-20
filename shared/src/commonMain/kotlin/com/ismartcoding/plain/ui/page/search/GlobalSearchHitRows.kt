package com.ismartcoding.plain.ui.page.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.platform.audioIsPlayingFlow
import com.ismartcoding.plain.platform.getApplicationIcon
import com.ismartcoding.plain.platform.getMediaItemUriString
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PIcon
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.base.dragselect.rememberDragSelectState
import com.ismartcoding.plain.ui.components.DocItem
import com.ismartcoding.plain.ui.components.NoteListItem
import com.ismartcoding.plain.ui.components.PackageListItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformImageViewWithUri
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.GlobalSearchHit
import com.ismartcoding.plain.ui.models.GlobalSearchSource
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VPackage
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.page.audio.components.AudioListItem
import com.ismartcoding.plain.ui.page.feeds.FeedClusterEntryRow
import com.ismartcoding.plain.ui.page.feeds.FeedListRow
import com.ismartcoding.plain.ui.page.files.components.FileListItem
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.theme.searchHighlight

/** Per-list dependencies for rows that reuse the source page's own list item component. */
class GlobalSearchRowContext(
    val navController: NavHostController,
    val audioPlaylistVM: AudioPlaylistViewModel,
    val audioVM: AudioViewModel,
    val tagsVM: TagsViewModel,
    val castVM: CastViewModel,
    val docsVM: DocsViewModel,
    val feedEntriesVM: FeedEntriesViewModel,
    val dragSelectState: DragSelectState,
    val itemState: TransformItemState,
    val previewerState: MediaPreviewerState,
)

@Composable
fun rememberGlobalSearchRowContext(
    navController: NavHostController,
    audioPlaylistVM: AudioPlaylistViewModel,
    audioVM: AudioViewModel,
    tagsVM: TagsViewModel,
    castVM: CastViewModel,
    docsVM: DocsViewModel,
    feedEntriesVM: FeedEntriesViewModel,
): GlobalSearchRowContext {
    val dragSelectState = rememberDragSelectState()
    val itemState = rememberTransformItemState()
    val previewerState = rememberPreviewerState()
    return remember(
        navController, audioPlaylistVM, audioVM, tagsVM, castVM, docsVM, feedEntriesVM,
        dragSelectState, itemState, previewerState,
    ) {
        GlobalSearchRowContext(
            navController, audioPlaylistVM, audioVM, tagsVM, castVM, docsVM, feedEntriesVM,
            dragSelectState, itemState, previewerState,
        )
    }
}

/** Dispatches a hit to the list item component of its source domain; falls back to the generic row. */
@Composable
fun GlobalSearchRow(
    hit: GlobalSearchHit,
    query: String,
    ctx: GlobalSearchRowContext,
    onOpen: (GlobalSearchHit) -> Unit,
    onPreviewMedia: (GlobalSearchHit, TransformItemState) -> Unit = { _, _ -> },
) {
    when (val src = hit.source) {
        is GlobalSearchSource.Note -> CardSpace {
            NoteListItem(
                m = src.note,
                tags = emptyList(),
                onClick = { onOpen(hit) },
                onLongClick = { },
                onClickTag = { },
            )
        }

        is GlobalSearchSource.Audio -> CardSpace {
            val isAudioPlaying by audioIsPlayingFlow().collectAsState()
            AudioListItem(
                item = src.audio,
                audioVM = ctx.audioVM,
                audioPlaylistVM = ctx.audioPlaylistVM,
                tagsVM = ctx.tagsVM,
                castVM = ctx.castVM,
                tags = emptyList(),
                dragSelectState = ctx.dragSelectState,
                isCurrentlyPlaying = isAudioPlaying && ctx.audioPlaylistVM.selectedPath.value == src.audio.path,
                isInPlaylist = ctx.audioPlaylistVM.isInPlaylist(src.audio.path),
            )
        }

        is GlobalSearchSource.Image, is GlobalSearchSource.Video -> {
            // Register the row thumbnail with the previewer's transform layer
            // (same as ImageGridItem) so open/close zoom to and from the thumb.
            val itemState = rememberTransformItemState()
            val widthPx = with(LocalDensity.current) { 40.dp.toPx() }.toInt()
            GlobalSearchHitRow(
                hit = hit,
                query = query,
                onOpen = { onPreviewMedia(hit, itemState) },
                thumb = {
                    when (src) {
                        is GlobalSearchSource.Image -> TransformImageViewWithUri(
                            modifier = Modifier.size(40.dp),
                            path = src.image.path,
                            fileName = src.image.path.getFilenameFromPath(),
                            key = src.image.id,
                            uri = getMediaItemUriString(DataType.IMAGE, src.image.id),
                            itemState = itemState,
                            previewerState = ctx.previewerState,
                            widthPx = widthPx,
                        )

                        is GlobalSearchSource.Video -> TransformImageViewWithUri(
                            modifier = Modifier.size(40.dp),
                            path = src.video.path,
                            fileName = src.video.path.getFilenameFromPath(),
                            key = src.video.id,
                            uri = getMediaItemUriString(DataType.VIDEO, src.video.id),
                            itemState = itemState,
                            previewerState = ctx.previewerState,
                            widthPx = widthPx,
                        )

                        else -> Unit
                    }
                },
            )
        }

        is GlobalSearchSource.Doc -> CardSpace {
            DocItem(
                navController = ctx.navController,
                docsVM = ctx.docsVM,
                dragSelectState = ctx.dragSelectState,
                m = src.doc,
                tags = emptyList(),
                onTagClick = { },
            )
        }

        is GlobalSearchSource.File -> FileListItem(
            file = src.file,
            isSelected = false,
            isSelectMode = false,
            itemState = ctx.itemState,
            previewerState = ctx.previewerState,
            onClick = { onOpen(hit) },
            onLongClick = { },
            audioPlaylistVM = ctx.audioPlaylistVM,
        )

        is GlobalSearchSource.Feed -> FeedClusterEntryRow(
            row = FeedListRow.Entry(key = hit.key, entry = src.entry),
            feedEntriesVM = ctx.feedEntriesVM,
            tags = emptyList(),
            indented = false,
            onClick = { onOpen(hit) },
            onLongClick = { },
            onClickTag = { },
        )

        is GlobalSearchSource.App -> CardSpace {
            PackageListItem(
                item = VPackage(
                    id = src.info.id,
                    name = src.info.name,
                    type = src.info.type,
                    version = src.info.version,
                    path = src.info.path,
                    size = src.info.size,
                    certs = src.info.certs,
                    installedAt = src.info.installedAt,
                    updatedAt = src.info.updatedAt,
                ),
                iconProvider = { getApplicationIcon(it) },
                modifier = PlainTheme.getCardModifier(),
                onClick = { onOpen(hit) },
            )
        }

        null -> GlobalSearchHitRow(
            hit = hit,
            query = query,
            onOpen = onOpen,
            thumb = { GlobalSearchThumb(hit) },
        )
    }
}

/** 8dp gap between stacked cards: 4dp below each card row. */
@Composable
private fun CardSpace(content: @Composable () -> Unit) {
    Box(Modifier.padding(vertical = 4.dp)) { content() }
}

/** Preview payload of an image/video hit, or null for other domains. */
fun GlobalSearchHit.previewItem(): PreviewItem? = when (val s = source) {
    is GlobalSearchSource.Image -> PreviewItem(s.image.id, s.image.path, s.image.size, mediaId = s.image.id, data = s.image)
    is GlobalSearchSource.Video -> PreviewItem(s.video.id, s.video.path, s.video.size, mediaId = s.video.id, data = s.video)
    else -> null
}

/** Generic result row (chat): leading icon, highlighted title, optional snippet and subtitle. */
@Composable
fun GlobalSearchHitRow(
    hit: GlobalSearchHit,
    query: String,
    onOpen: (GlobalSearchHit) -> Unit,
    thumb: @Composable () -> Unit,
) {
    val highlight = MaterialTheme.colorScheme.searchHighlight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onOpen(hit) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = if (hit.snippet.isEmpty()) Alignment.CenterVertically else Alignment.Top,
    ) {
        thumb()
        HorizontalSpace(12.dp)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp),
        ) {
            if (hit.snippet.isEmpty()) {
                Text(
                    text = remember(hit.key, query) { highlightQuery(hit.title, query, highlight) },
                    style = MaterialTheme.typography.listItemTitle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (hit.subtitle.isNotEmpty()) {
                    VerticalSpace(2.dp)
                    Text(
                        text = hit.subtitle,
                        style = MaterialTheme.typography.listItemSubtitle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = remember(hit.key, query) { highlightQuery(hit.title, query, highlight) },
                        style = MaterialTheme.typography.listItemTitle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (hit.subtitle.isNotEmpty()) {
                        HorizontalSpace(8.dp)
                        Text(
                            text = hit.subtitle,
                            style = MaterialTheme.typography.listItemSubtitle(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                VerticalSpace(2.dp)
                Text(
                    text = remember(hit.key + "s", query) { highlightQuery(hit.snippet, query, highlight) },
                    style = MaterialTheme.typography.listItemSubtitle(),
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun GlobalSearchThumb(hit: GlobalSearchHit) {
    val iconRes = hit.iconRes ?: return
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(if (hit.roundThumb) CircleShape else RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.cardBackgroundNormal),
        contentAlignment = Alignment.Center,
    ) {
        PIcon(icon = iconRes, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}
