package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.helpers.launchSafe
import com.ismartcoding.plain.lib.LrcWriter
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.WhisperModelSpec
import com.ismartcoding.plain.platform.WhisperModels
import com.ismartcoding.plain.platform.cancelWhisperModelDownload
import com.ismartcoding.plain.platform.closeWhisperEngine
import com.ismartcoding.plain.platform.downloadWhisperModel
import com.ismartcoding.plain.platform.getWhisperModelPath
import com.ismartcoding.plain.platform.isWhisperModelDownloaded
import com.ismartcoding.plain.platform.loadWhisperEngine
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.platform.transcribeWhisper
import com.ismartcoding.plain.platform.findExistingLyricsFile
import com.ismartcoding.plain.platform.resolveAudioRealPath
import com.ismartcoding.plain.platform.writeLyricsFile
import com.ismartcoding.plain.preferences.WhisperModelPreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LyricsExtractViewModel : ViewModel() {
    enum class JobState { PENDING, RUNNING, DONE, NO_SPEECH, FAILED }

    data class JobItem(
        val audio: DAudio,
        val realPath: String?,
        val state: JobState = JobState.PENDING,
        val progress: Float = 0f,
        val error: String? = null,
    )

    data class DownloadState(
        val running: Boolean = false,
        val downloaded: Long = 0,
        val total: Long = 0,
        val failed: Boolean = false,
    )

    val items = MutableStateFlow<List<JobItem>>(emptyList())
    val scanned = mutableStateOf(false)
    val running = MutableStateFlow(false)
    val paused = MutableStateFlow(false)
    val selectedModel = MutableStateFlow(WhisperModels.byId(WhisperModelPreference.default))
    val downloadedIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadState = MutableStateFlow(DownloadState())
    val language = mutableStateOf("auto")
    val error = mutableStateOf("")

    private var engine: com.ismartcoding.plain.platform.WhisperEngineHandle? = null
    private var queueJob: Job? = null
    private var scanJob: Job? = null

    init {
        viewModelScope.launchSafe {
            selectedModel.value = runCatching { WhisperModels.byId(WhisperModelPreference.getAsync()) }
                .getOrDefault(WhisperModels.byId(WhisperModelPreference.default))
            refreshDownloaded()
        }
    }

    fun refreshDownloaded() {
        viewModelScope.launchSafe {
            val ids = WhisperModels.ALL.filter { isWhisperModelDownloaded(it) }.map { it.id }.toSet()
            downloadedIds.value = ids
        }
    }

    fun selectModel(spec: WhisperModelSpec) {
        selectedModel.value = spec
        viewModelScope.launchSafe { WhisperModelPreference.putAsync(spec.id) }
    }

    fun downloadModel(spec: WhisperModelSpec) {
        if (downloadState.value.running) return
        viewModelScope.launchSafe {
            downloadState.value = DownloadState(running = true)
            cancelWhisperModelDownload()
            val result = downloadWhisperModel(spec) { downloaded, total ->
                downloadState.value = DownloadState(running = true, downloaded = downloaded, total = total)
            }
            result.fold(
                onSuccess = {
                    downloadState.value = DownloadState()
                    refreshDownloaded()
                    selectModel(spec)
                },
                onFailure = { e ->
                    downloadState.value = DownloadState(failed = true)
                    error.value = e.message ?: "download failed"
                },
            )
        }
    }

    /** Scans the audio library for songs without a sibling .lrc file. */
    fun scanAsync() {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launchSafe {
            val audios = withIO {
                searchMedia(DataType.AUDIO, "trash:false", 1000, 0, FileSortBy.DATE_DESC)
                    .filterIsInstance<DAudio>()
            }
            val jobs = audios.mapNotNull { audio ->
                val existing = findExistingLyricsFile(audio.path)
                if (existing != null) return@mapNotNull null
                val real = resolveAudioRealPath(audio.path)
                JobItem(audio = audio, realPath = real)
            }
            items.value = jobs
            scanned.value = true
        }
    }

    fun extractAsync(audio: DAudio) {
        viewModelScope.launchSafe {
            val list = items.value.toMutableList()
            val existingIndex = list.indexOfFirst { it.audio.path == audio.path }
            val realPath = list.getOrNull(existingIndex)?.realPath ?: resolveAudioRealPath(audio.path)
            val item = JobItem(audio = audio, realPath = realPath)
            if (existingIndex >= 0) {
                if (list[existingIndex].state == JobState.RUNNING) return@launchSafe
                list[existingIndex] = item.copy(state = JobState.PENDING)
            } else {
                list.add(0, item)
            }
            items.value = list
            startQueue()
        }
    }

    fun extractAllAsync() {
        val list = items.value.map { if (it.state != JobState.RUNNING) it.copy(state = JobState.PENDING) else it }
        items.value = list
        startQueue()
    }

    fun retryAsync(audio: DAudio) {
        val list = items.value.toMutableList()
        val index = list.indexOfFirst { it.audio.path == audio.path }
        if (index >= 0) {
            list[index] = list[index].copy(state = JobState.PENDING, progress = 0f, error = null)
            items.value = list
            startQueue()
        }
    }

    /** Finishes the current file, then holds the queue. */
    fun pause() {
        paused.value = true
    }

    fun resume() {
        paused.value = false
        startQueue()
    }

    /** Aborts the current file (back to pending) and stops the queue. */
    fun stop() {
        paused.value = false
        queueJob?.cancel()
        queueJob = null
    }

    private fun startQueue() {
        if (queueJob?.isActive == true) return
        queueJob = viewModelScope.launchSafe {
            running.value = true
            try {
                val modelPath = getWhisperModelPath(selectedModel.value)
                if (modelPath == null) {
                    error.value = "model not downloaded"
                    return@launchSafe
                }
                val engineHandle = engine ?: loadWhisperEngine(modelPath).also { engine = it }
                while (!paused.value) {
                    val next = items.value.firstOrNull { it.state == JobState.PENDING } ?: break
                    runOne(engineHandle, next)
                }
            } catch (_: CancellationException) {
                // stop(): states reset below
            } finally {
                engine?.let { closeWhisperEngine(it) }
                engine = null
                running.value = false
            }
        }
    }

    private suspend fun runOne(engineHandle: com.ismartcoding.plain.platform.WhisperEngineHandle, job: JobItem) {
        update(job.audio.path) { it.copy(state = JobState.RUNNING, progress = 0f, error = null) }
        try {
            val realPath = job.realPath ?: throw IllegalStateException("file not accessible")
            val transcription = transcribeWhisper(
                engine = engineHandle,
                audioPath = realPath,
                durationMs = job.audio.duration,
                language = language.value,
            ) { progress ->
                update(job.audio.path) { it.copy(progress = progress) }
            }
            if (!transcription.hasSpeech) {
                update(job.audio.path) { it.copy(state = JobState.NO_SPEECH, progress = 1f) }
            } else {
                val lrc = LrcWriter.write(transcription.segments.map { com.ismartcoding.plain.lib.LrcParser.LrcLine(it.timeMs, it.text) })
                val written = writeLyricsFile(realPath, lrc)
                if (written != null) {
                    update(job.audio.path) { it.copy(state = JobState.DONE, progress = 1f) }
                } else {
                    update(job.audio.path) { it.copy(state = JobState.FAILED, error = "cannot write lyrics file") }
                }
            }
        } catch (e: CancellationException) {
            update(job.audio.path) { it.copy(state = JobState.PENDING, progress = 0f) }
            throw e
        } catch (e: Exception) {
            update(job.audio.path) { it.copy(state = JobState.FAILED, error = e.message ?: "failed") }
        }
    }

    private fun update(path: String, transform: (JobItem) -> JobItem) {
        items.value = items.value.map { if (it.audio.path == path) transform(it) else it }
    }
}
