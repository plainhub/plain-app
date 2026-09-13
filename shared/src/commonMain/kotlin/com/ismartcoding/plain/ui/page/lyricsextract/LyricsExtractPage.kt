@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.page.lyricsextract

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.audio_lines
import com.ismartcoding.plain.i18n.download
import com.ismartcoding.plain.i18n.auto_detect
import com.ismartcoding.plain.i18n.change_model
import com.ismartcoding.plain.i18n.choose_model
import com.ismartcoding.plain.i18n.close
import com.ismartcoding.plain.i18n.x
import com.ismartcoding.plain.i18n.current_song
import com.ismartcoding.plain.i18n.done
import com.ismartcoding.plain.i18n.extract
import com.ismartcoding.plain.i18n.extract_all
import com.ismartcoding.plain.i18n.extract_failed
import com.ismartcoding.plain.i18n.extract_lyrics
import com.ismartcoding.plain.i18n.extract_lyrics_hint
import com.ismartcoding.plain.i18n.model_desc_base
import com.ismartcoding.plain.i18n.model_desc_small
import com.ismartcoding.plain.i18n.model_desc_tiny
import com.ismartcoding.plain.i18n.model_download_failed
import com.ismartcoding.plain.i18n.model_download_hint
import com.ismartcoding.plain.i18n.model_downloaded
import com.ismartcoding.plain.i18n.model_downloading
import com.ismartcoding.plain.i18n.model_not_downloaded
import com.ismartcoding.plain.i18n.model_privacy_note
import com.ismartcoding.plain.i18n.model_ready
import com.ismartcoding.plain.i18n.music2
import com.ismartcoding.plain.i18n.no_speech
import com.ismartcoding.plain.i18n.pause
import com.ismartcoding.plain.i18n.recognized_language
import com.ismartcoding.plain.i18n.resume
import com.ismartcoding.plain.i18n.retry
import com.ismartcoding.plain.i18n.songs_without_lyrics
import com.ismartcoding.plain.i18n.stop
import com.ismartcoding.plain.i18n.waiting
import com.ismartcoding.plain.platform.WhisperModelSpec
import com.ismartcoding.plain.platform.WhisperModels
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.models.LyricsExtractViewModel
import com.ismartcoding.plain.ui.page.audio.components.AudioCoverOrIcon
import com.ismartcoding.plain.ui.theme.green
import kotlin.time.Instant
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val LANGUAGE_OPTIONS = listOf(
    "auto" to "Auto",
    "zh" to "中文",
    "en" to "English",
    "ja" to "日本語",
    "ko" to "한국어",
    "yue" to "粵語",
    "es" to "Español",
    "fr" to "Français",
    "de" to "Deutsch",
    "ru" to "Русский",
)

@Composable
fun LyricsExtractPage(
    navController: NavHostController,
    audioPlaylistVM: AudioPlaylistViewModel,
    vm: LyricsExtractViewModel = viewModel(key = "lyricsExtractVM") { LyricsExtractViewModel() },
) {
    val items by vm.items.collectAsState()
    val running by vm.running.collectAsState()
    val paused by vm.paused.collectAsState()
    val selectedModel by vm.selectedModel.collectAsState()
    val downloadedIds by vm.downloadedIds.collectAsState()
    val downloadState by vm.downloadState.collectAsState()

    LaunchedEffect(Unit) { vm.scanAsync() }

    var showModelSheet by remember { mutableStateOf(false) }
    val currentPlaying = remember(items, audioPlaylistVM.playlistItems.value, audioPlaylistVM.selectedPath.value) {
        audioPlaylistVM.playlistItems.value
            .firstOrNull { it.path == audioPlaylistVM.selectedPath.value }
            ?.let { p ->
                items.firstOrNull { it.audio.path == p.path }?.audio
                    ?: DAudio(
                        id = p.path,
                        title = p.title,
                        artist = p.artist,
                        path = p.path,
                        duration = p.duration,
                        size = 0,
                        bucketId = "",
                        albumId = "",
                        createdAt = Instant.fromEpochMilliseconds(0),
                        updatedAt = Instant.fromEpochMilliseconds(0),
                    )
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PTopAppBar(navController = navController, title = stringResource(Res.string.extract_lyrics))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ModelCard(
                    selectedModel = selectedModel,
                    downloadedIds = downloadedIds,
                    downloadState = downloadState,
                    onSelect = { showModelSheet = true },
                )
            }
            item {
                PCard {
                    LanguageRow(vm)
                }
            }
            if (currentPlaying != null) {
                item {
                    Column {
                        SectionHeader(stringResource(Res.string.current_song))
                        Spacer(Modifier.height(8.dp))
                        PCard {
                            SongRow(
                                audio = currentPlaying,
                                job = items.firstOrNull { it.audio.path == currentPlaying.path },
                                enabled = downloadedIds.isNotEmpty(),
                                onExtract = { vm.extractAsync(currentPlaying) },
                            )
                        }
                    }
                }
            }
            item {
                SectionHeader(stringResource(Res.string.songs_without_lyrics, items.size))
            }
            if (items.isEmpty()) {
                item { EmptyHint() }
            }
            items(items.size) { index ->
                val job = items[index]
                PCard {
                    SongRow(
                        audio = job.audio,
                        job = job,
                        enabled = downloadedIds.isNotEmpty(),
                        onExtract = { vm.extractAsync(job.audio) },
                    )
                }
            }
            if (items.any { it.state == LyricsExtractViewModel.JobState.PENDING }) {
                item {
                    if (running) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PFilledButton(
                                text = stringResource(if (paused) Res.string.resume else Res.string.pause),
                                onClick = { if (paused) vm.resume() else vm.pause() },
                                modifier = Modifier.weight(1f),
                            )
                            PFilledButton(
                                text = stringResource(Res.string.stop),
                                onClick = { vm.stop() },
                                type = com.ismartcoding.plain.enums.ButtonType.DANGER,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        PFilledButton(
                            text = stringResource(Res.string.extract_all),
                            onClick = { vm.extractAllAsync() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showModelSheet) {
        ModelPickerSheet(
            selected = selectedModel,
            downloadedIds = downloadedIds,
            downloadState = downloadState,
            onSelect = {
                vm.selectModel(it)
                showModelSheet = false
            },
            onDownload = { vm.downloadModel(it) },
            onClose = { showModelSheet = false },
        )
    }
}

@Composable
private fun ModelCard(
    selectedModel: WhisperModelSpec,
    downloadedIds: Set<String>,
    downloadState: LyricsExtractViewModel.DownloadState,
    onSelect: () -> Unit,
) {
    val downloaded = selectedModel.id in downloadedIds
    PCard(
        modifier = if (downloaded) {
            Modifier
        } else {
            Modifier.border(
                1.5.dp,
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(PlainTheme.CARD_RADIUS),
            )
        },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.audio_lines),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (downloaded) {
                            "${whisperDisplayName(selectedModel)} · ${stringResource(Res.string.model_ready)}"
                        } else {
                            stringResource(Res.string.model_not_downloaded)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = if (downloaded) formatSize(selectedModel.sizeBytes) else stringResource(Res.string.model_download_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (downloaded) {
                    PFilledButton(text = stringResource(Res.string.change_model), onClick = onSelect, buttonSize = ButtonSize.MEDIUM)
                } else {
                    PFilledButton(text = stringResource(Res.string.choose_model), onClick = onSelect, buttonSize = ButtonSize.MEDIUM)
                }
            }
            if (downloadState.running && downloadState.total > 0) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { (downloadState.downloaded.toFloat() / downloadState.total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${stringResource(Res.string.model_downloading)} ${formatSize(downloadState.downloaded)} / ${formatSize(downloadState.total)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else if (downloadState.failed) {
                Text(
                    text = stringResource(Res.string.model_download_failed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.model_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModelPickerSheet(
    selected: WhisperModelSpec,
    downloadedIds: Set<String>,
    downloadState: LyricsExtractViewModel.DownloadState,
    onSelect: (WhisperModelSpec) -> Unit,
    onDownload: (WhisperModelSpec) -> Unit,
    onClose: () -> Unit,
) {
    PModalBottomSheet(onDismissRequest = onClose) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.choose_model),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
            )
            PIconButton(icon = Res.drawable.x, contentDescription = stringResource(Res.string.close), click = onClose)
        }
        WhisperModels.ALL.forEach { spec ->
            ModelRow(
                spec = spec,
                selected = spec.id == selected.id,
                downloaded = spec.id in downloadedIds,
                downloading = downloadState.running && spec.id == selected.id && spec.id !in downloadedIds,
                onSelect = onSelect,
                onDownload = onDownload,
            )
        }
        Text(
            text = stringResource(Res.string.model_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ModelRow(
    spec: WhisperModelSpec,
    selected: Boolean,
    downloaded: Boolean,
    downloading: Boolean,
    onSelect: (WhisperModelSpec) -> Unit,
    onDownload: (WhisperModelSpec) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = downloaded) { onSelect(spec) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = whisperDisplayName(spec),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            )
            Text(
                text = "${modelDescription(spec)} · ${formatSize(spec.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            selected && downloaded -> Text(
                text = stringResource(Res.string.model_ready),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            downloaded -> Text(
                text = stringResource(Res.string.model_downloaded),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            downloading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else -> PFilledButton(
                text = stringResource(Res.string.download),
                onClick = { onDownload(spec) },
                icon = painterResource(Res.drawable.download),
                buttonSize = ButtonSize.SMALL,
            )
        }
    }
}

@Composable
private fun SongRow(
    audio: DAudio,
    job: LyricsExtractViewModel.JobItem?,
    enabled: Boolean,
    onExtract: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AudioCoverOrIcon(
            path = audio.path,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audio.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = audio.getSubtitle(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        SongState(state = job?.state, progress = job?.progress ?: 0f, enabled = enabled, onExtract = onExtract)
    }
}

@Composable
private fun SongState(
    state: LyricsExtractViewModel.JobState?,
    progress: Float,
    enabled: Boolean,
    onExtract: () -> Unit,
) {
    when (state) {
        LyricsExtractViewModel.JobState.RUNNING -> {
            Column(horizontalAlignment = Alignment.End) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.width(80.dp),
                )
                Text(
                    text = "${(progress * 100).toInt().coerceIn(0, 100)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        LyricsExtractViewModel.JobState.PENDING -> Text(
            text = stringResource(Res.string.waiting),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LyricsExtractViewModel.JobState.DONE -> Text(
            text = stringResource(Res.string.done),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.green,
        )
        LyricsExtractViewModel.JobState.NO_SPEECH -> Text(
            text = stringResource(Res.string.no_speech),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LyricsExtractViewModel.JobState.FAILED -> PFilledButton(
            text = stringResource(Res.string.retry),
            onClick = onExtract,
            buttonSize = ButtonSize.SMALL,
        )
        null -> PFilledButton(
            text = stringResource(Res.string.extract),
            onClick = onExtract,
            buttonSize = ButtonSize.SMALL,
            enabled = enabled,
        )
    }
}

@Composable
private fun LanguageRow(vm: LyricsExtractViewModel) {
    var expanded by remember { mutableStateOf(false) }
    PListItem(
        title = stringResource(Res.string.recognized_language),
        value = languageLabel(vm.language.value),
        action = {
            TextButton(onClick = { expanded = true }) {
                Text(languageLabel(vm.language.value))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                LANGUAGE_OPTIONS.forEach { (code, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            vm.language.value = code
                            expanded = false
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun languageLabel(code: String): String {
    if (code == "auto") return stringResource(Res.string.auto_detect)
    return LANGUAGE_OPTIONS.firstOrNull { it.first == code }?.second ?: code
}

private fun whisperDisplayName(spec: WhisperModelSpec): String =
    "Whisper " + spec.id.replaceFirstChar { it.uppercase() }

@Composable
private fun modelDescription(spec: WhisperModelSpec): String = when (spec.id) {
    "tiny" -> stringResource(Res.string.model_desc_tiny)
    "small" -> stringResource(Res.string.model_desc_small)
    else -> stringResource(Res.string.model_desc_base)
}

private fun formatSize(bytes: Long): String {
    val mb = (bytes + (1024 * 1024) - 1) / (1024 * 1024)
    return "$mb MB"
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EmptyHint() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.music2),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = stringResource(Res.string.extract_lyrics_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp).padding(horizontal = 24.dp),
        )
    }
}
