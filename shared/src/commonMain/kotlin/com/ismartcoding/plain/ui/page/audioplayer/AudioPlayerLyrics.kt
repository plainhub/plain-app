package com.ismartcoding.plain.ui.page.audioplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.extract_lyrics
import com.ismartcoding.plain.i18n.extract_lyrics_hint
import com.ismartcoding.plain.i18n.music2
import com.ismartcoding.plain.i18n.no_lyrics
import com.ismartcoding.plain.i18n.sparkles
import com.ismartcoding.plain.lib.LrcParser
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.getAudioLyrics
import com.ismartcoding.plain.platform.readTextFile
import com.ismartcoding.plain.ui.base.PFilledButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Sibling .lrc file path for an audio file path. content:// URIs have no
 * sibling file, return them unchanged so the read fails softly.
 */
private fun lyricsPathFor(audioPath: String): String {
    if (audioPath.startsWith("content://")) return audioPath
    val dot = audioPath.lastIndexOf('.')
    val slash = audioPath.lastIndexOf('/')
    return if (dot > slash) audioPath.substring(0, dot) + ".lrc" else "$audioPath.lrc"
}

/** Sidecar .lrc file first, lyrics embedded in the audio metadata as fallback. */
suspend fun loadLyrics(audioPath: String): List<LrcParser.LrcLine> =
    withIO {
        LrcParser.parse(readTextFile(lyricsPathFor(audioPath)).ifEmpty { getAudioLyrics(audioPath) })
    }

/** Sentinel default so the extraction CTA only appears when a handler is wired. */
private val PlayerLyricsNoOp: () -> Unit = {}

/**
 * Auto-scrolling lyrics view. [lines] is the parsed LRC content (null while
 * loading, empty when no lyrics exist); [progressMs] is the current playback
 * position. The active line is bold on-surface, the others on-surface-variant,
 * and tapping a line seeks to its timestamp. When lyrics are missing,
 * [onNavigateToExtractLyrics] offers the on-device extraction flow.
 */
@Composable
fun AudioPlayerLyrics(
    lines: List<LrcParser.LrcLine>?,
    progressMs: Long,
    onSeek: (Long) -> Unit,
    onNavigateToExtractLyrics: () -> Unit = PlayerLyricsNoOp,
    modifier: Modifier = Modifier,
) {
    val listState = remember(lines) { LazyListState() }

    if (lines == null) {
        Box(modifier.fillMaxSize())
        return
    }
    if (lines.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.music2),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp),
                    )
                }
                Text(
                    text = stringResource(Res.string.no_lyrics),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Text(
                    text = stringResource(Res.string.extract_lyrics_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp).padding(horizontal = 32.dp),
                )
                if (onNavigateToExtractLyrics !== PlayerLyricsNoOp) {
                    PFilledButton(
                        text = stringResource(Res.string.extract_lyrics),
                        onClick = onNavigateToExtractLyrics,
                        icon = painterResource(Res.drawable.sparkles),
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
        return
    }

    val activeIndex = lines.indexOfLast { it.timeMs <= progressMs }

    // Tapping a line must never scroll the list: the tapped line is already
    // on screen. Follow-scrolling pauses on tap and resumes a moment later,
    // once the seek has settled.
    var suppressFollow by remember(lines) { mutableStateOf(false) }
    var suppressJob by remember(lines) { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(activeIndex) {
        if (activeIndex < 0 || suppressFollow) return@LaunchedEffect
        val info = listState.layoutInfo
        val viewport = info.viewportEndOffset - info.viewportStartOffset
        val itemHeight = info.visibleItemsInfo.firstOrNull { it.index == activeIndex }?.size ?: 0
        listState.animateScrollToItem(activeIndex, -(viewport / 2 - itemHeight / 2))
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 160.dp),
    ) {
        itemsIndexed(lines) { index, line ->
            val active = index == activeIndex
            Text(
                text = line.text,
                style = if (active) MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                else MaterialTheme.typography.titleMedium,
                color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        suppressJob?.cancel()
                        suppressFollow = true
                        suppressJob = scope.launch {
                            delay(1200)
                            suppressFollow = false
                        }
                        onSeek(line.timeMs)
                    }
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            )
        }
    }
}
