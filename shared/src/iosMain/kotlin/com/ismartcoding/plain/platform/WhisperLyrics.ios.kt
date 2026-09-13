package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.withIO

// Whisper lyrics extraction ships on Android first; these stubs keep the iOS
// target compiling. The extraction UI gates itself on model availability.

actual suspend fun isWhisperModelDownloaded(spec: WhisperModelSpec): Boolean = false

actual suspend fun getWhisperModelPath(spec: WhisperModelSpec): String? = null

actual suspend fun downloadWhisperModel(
    spec: WhisperModelSpec,
    onProgress: (Long, Long) -> Unit,
): Result<String> = Result.failure(UnsupportedOperationException("whisper extraction is not available on this platform"))

actual fun cancelWhisperModelDownload() = Unit

actual suspend fun loadWhisperEngine(modelPath: String): WhisperEngineHandle =
    throw UnsupportedOperationException("whisper extraction is not available on this platform")

actual fun closeWhisperEngine(engine: WhisperEngineHandle) = Unit

actual suspend fun transcribeWhisper(
    engine: WhisperEngineHandle,
    audioPath: String,
    durationMs: Long,
    language: String,
    onProgress: (Float) -> Unit,
): WhisperTranscription = withIO { WhisperTranscription(emptyList(), false) }

actual suspend fun resolveAudioRealPath(path: String): String? = withIO { path.takeIf { it.startsWith("/") } }

actual suspend fun findExistingLyricsFile(audioPath: String): String? = withIO {
    val real = resolveAudioRealPath(audioPath) ?: return@withIO null
    val sibling = lyricsSiblingPath(real)
    if (fileExists(sibling)) sibling else null
}

actual suspend fun writeLyricsFile(audioPath: String, content: String): String? = withIO {
    val real = resolveAudioRealPath(audioPath) ?: return@withIO null
    val target = lyricsSiblingPath(real)
    writeFileText(target, content, overwrite = true)
    target
}
