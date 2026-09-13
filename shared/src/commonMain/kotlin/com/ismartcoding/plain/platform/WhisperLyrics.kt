package com.ismartcoding.plain.platform

private const val MODELS_BASE_URL = "https://d.plainapp.app/models"

/** One downloadable on-device speech recognition model. */
data class WhisperModelSpec(
    val id: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
) {
    val url: String get() = "$MODELS_BASE_URL/$fileName"
}

object WhisperModels {
    // checksums of https://d.plainapp.app/models/<file> (huggingface ggerganov/whisper.cpp ggml)
    val TINY = WhisperModelSpec(
        "tiny", "ggml-tiny.bin", 77691713L,
        "be07e048e1e599ad46341c8d2a135645097a538221678b7acdd1b1919c6e1b21",
    )
    val BASE = WhisperModelSpec(
        "base", "ggml-base.bin", 147951465L,
        "60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe",
    )
    val SMALL = WhisperModelSpec(
        "small", "ggml-small.bin", 487601967L,
        "1be3a9b2063867b937e64e2ec7483364a79917e157fa98c5d94b5c1fffea987b",
    )
    val ALL = listOf(TINY, BASE, SMALL)

    fun byId(id: String): WhisperModelSpec = ALL.first { it.id == id }
}

/** Recognition result for one audio file. */
data class WhisperTranscription(
    val segments: List<WhisperSegment>,
    /** False when the recognition produced no usable text (instrumental audio). */
    val hasSpeech: Boolean,
)

data class WhisperSegment(val timeMs: Long, val endMs: Long, val text: String)

/** Loaded model. Expensive — keep one per batch and close when idle. */
class WhisperEngineHandle internal constructor(internal val ptr: Long)

expect suspend fun isWhisperModelDownloaded(spec: WhisperModelSpec): Boolean

/** Path of the downloaded model file, null when absent. */
expect suspend fun getWhisperModelPath(spec: WhisperModelSpec): String?

/**
 * Downloads the model with resume support, verifies size and sha256, and
 * reports (downloadedBytes, totalBytes). Cancels a prior running download.
 */
expect suspend fun downloadWhisperModel(
    spec: WhisperModelSpec,
    onProgress: (downloaded: Long, total: Long) -> Unit,
): Result<String>

expect fun cancelWhisperModelDownload()

/** Loads a model into memory. Failures throw. */
expect suspend fun loadWhisperEngine(modelPath: String): WhisperEngineHandle

expect fun closeWhisperEngine(engine: WhisperEngineHandle)

/**
 * Transcribes an audio file to timed segments. [durationMs] feeds progress;
 * [language] is an ISO code or "auto". Progress is 0..1.
 */
expect suspend fun transcribeWhisper(
    engine: WhisperEngineHandle,
    audioPath: String,
    durationMs: Long,
    language: String,
    onProgress: (Float) -> Unit,
): WhisperTranscription

/** Real filesystem path for an audio path (content URIs resolved via MediaStore), null if unresolvable. */
expect suspend fun resolveAudioRealPath(path: String): String?

/** Sibling .lrc file path when one already exists on disk, else null. */
expect suspend fun findExistingLyricsFile(audioPath: String): String?

/** Writes lyrics next to the audio file; returns the written path or null when impossible. */
expect suspend fun writeLyricsFile(audioPath: String, content: String): String?

/** Sibling .lrc path convention shared by the player loader and the extractor. */
fun lyricsSiblingPath(audioRealPath: String): String {
    val dot = audioRealPath.lastIndexOf('.')
    val slash = audioRealPath.lastIndexOf('/')
    return if (dot > slash) audioRealPath.substring(0, dot) + ".lrc" else "$audioRealPath.lrc"
}
