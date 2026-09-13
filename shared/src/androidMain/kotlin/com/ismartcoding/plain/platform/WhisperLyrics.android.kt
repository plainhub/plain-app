package com.ismartcoding.plain.platform

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.StatFs
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.api.OkHttpClientFactory
import com.ismartcoding.plain.lib.AudioResampler
import com.ismartcoding.plain.lib.RawSegment
import com.ismartcoding.plain.lib.TranscribeMerge
import com.ismartcoding.plain.lib.extensions.pathToUri
import com.ismartcoding.plain.lib.withIO
import java.io.File
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.Request

private val modelsDir: File get() = File(appContext.filesDir, "whisper").apply { mkdirs() }

private fun modelFile(spec: WhisperModelSpec) = File(modelsDir, spec.fileName)

actual suspend fun isWhisperModelDownloaded(spec: WhisperModelSpec): Boolean = withIO {
    val f = modelFile(spec)
    f.exists() && f.length() == spec.sizeBytes
}

actual suspend fun getWhisperModelPath(spec: WhisperModelSpec): String? = withIO {
    if (isWhisperModelDownloaded(spec)) modelFile(spec).absolutePath else null
}

private var downloadCall: okhttp3.Call? = null

actual suspend fun downloadWhisperModel(
    spec: WhisperModelSpec,
    onProgress: (Long, Long) -> Unit,
): Result<String> = withIO {
    val file = modelFile(spec)
    val stat = StatFs(appContext.filesDir.path)
    if (stat.availableBytes < spec.sizeBytes * 2) {
        return@withIO Result.failure(IllegalStateException("insufficient storage"))
    }
    val client = OkHttpClientFactory.downloadClient()
    try {
        var downloaded = if (file.exists() && file.length() < spec.sizeBytes) file.length() else 0L
        if (file.length() > spec.sizeBytes) file.delete()
        val requestBuilder = Request.Builder().url(spec.url)
        if (downloaded > 0) requestBuilder.header("Range", "bytes=$downloaded-")
        val call = client.newCall(requestBuilder.build())
        downloadCall = call
        call.execute().use { response ->
            check(response.isSuccessful) { "download failed: HTTP ${response.code}" }
            val body = response.body
            val total = if (response.code == 206) spec.sizeBytes else body.contentLength().takeIf { it > 0 } ?: spec.sizeBytes
            body.source().use { source ->
                java.io.FileOutputStream(file, downloaded > 0).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        ensureActive()
                        val read = source.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded.coerceAtMost(total), total)
                    }
                }
            }
        }
        check(file.length() == spec.sizeBytes) { "size mismatch: ${file.length()} != ${spec.sizeBytes}" }
        val actual = sha256File(file.absolutePath)
        check(actual == spec.sha256) { "checksum mismatch: $actual" }
        Result.success(file.absolutePath)
    } catch (e: CancellationException) {
        downloadCall?.cancel()
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    } finally {
        downloadCall = null
    }
}

actual fun cancelWhisperModelDownload() {
    downloadCall?.cancel()
}

actual suspend fun loadWhisperEngine(modelPath: String): WhisperEngineHandle = withContext(Dispatchers.Default) {
    val ptr = WhisperJni.nativeLoadModel(modelPath)
    check(ptr != 0L) { "failed to load whisper model: $modelPath" }
    WhisperEngineHandle(ptr)
}

actual fun closeWhisperEngine(engine: WhisperEngineHandle) {
    WhisperJni.nativeFreeModel(engine.ptr)
}

private const val CHUNK_SECONDS = 90
private const val OVERLAP_SECONDS = 2
private const val SAMPLE_RATE = 16000

/**
 * Transcribes one audio file by decoding it into [CHUNK_SECONDS] chunks (with
 * an [OVERLAP_SECONDS] tail overlap so words are not cut at boundaries) and
 * running whisper over each chunk.
 */
actual suspend fun transcribeWhisper(
    engine: WhisperEngineHandle,
    audioPath: String,
    durationMs: Long,
    language: String,
    onProgress: (Float) -> Unit,
): WhisperTranscription = withContext(Dispatchers.Default) {
    val segments = mutableListOf<RawSegment>()
    val cancelled = AtomicBoolean(false)
    val listener = WhisperJni.ProgressListener { _ ->
        val active = coroutineContext.isActive
        if (!active) cancelled.set(true)
        active
    }
    val threads = min(4, max(2, Runtime.getRuntime().availableProcessors() / 2))
    val stepMs = (CHUNK_SECONDS - OVERLAP_SECONDS) * 1000L

    decodeAudioToMono16k(audioPath) { chunk, count, index ->
        ensureActive()
        val raw = WhisperJni.nativeRun(engine.ptr, chunk, count, language, threads, listener)
        if (raw == null) {
            if (cancelled.get()) throw CancellationException("transcription cancelled")
            throw IllegalStateException("whisper transcription failed")
        }
        val chunkStartMs = index * stepMs
        var adjusted = TranscribeMerge.shift(
            raw.map { RawSegment(it.t0 * 10, it.t1 * 10, String(it.text, Charsets.UTF_8).trim()) },
            chunkStartMs,
        )
        if (index > 0) adjusted = TranscribeMerge.dropOverlap(adjusted, chunkStartMs + OVERLAP_SECONDS * 1000L)
        segments += adjusted
        if (durationMs > 0) onProgress((chunkStartMs.toFloat() / durationMs).coerceIn(0f, 1f))
    }
    onProgress(1f)

    val usable = segments.filter { it.text.length >= 2 }
    WhisperTranscription(
        segments = usable.map { WhisperSegment(it.timeMs, it.endMs, it.text) },
        hasSpeech = usable.isNotEmpty(),
    )
}

/**
 * Streaming decode: audio file → mono float PCM at 16 kHz, delivered in
 * [CHUNK_SECONDS] blocks (last block may be shorter). Chunk i > 0 begins
 * [OVERLAP_SECONDS] before its nominal start. Throws on cancellation.
 */
private fun decodeAudioToMono16k(
    path: String,
    onChunk: (chunk: FloatArray, count: Int, chunkIndex: Int) -> Unit,
) {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    var afd: android.content.res.AssetFileDescriptor? = null
    try {
        if (path.startsWith("/")) extractor.setDataSource(path)
        else {
            afd = appContext.contentResolver.openAssetFileDescriptor(path.pathToUri(), "r")
                ?: throw IllegalStateException("cannot open $path")
            extractor.setDataSource(afd)
        }
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                trackIndex = i
                format = f
                break
            }
        }
        if (trackIndex < 0 || format == null) throw IllegalStateException("no audio track: $path")
        extractor.selectTrack(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val decoder = MediaCodec.createDecoderByType(mime).also { codec = it }
        decoder.configure(format, null, null, 0)
        decoder.start()

        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var resampler = AudioResampler(sampleRate, SAMPLE_RATE)
        val chunker = ChunkBuffer(
            chunkSamples = CHUNK_SECONDS * SAMPLE_RATE,
            stepSamples = (CHUNK_SECONDS - OVERLAP_SECONDS) * SAMPLE_RATE,
        )
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var mono = FloatArray(8192)

        while (!outputDone) {
            if (!inputDone) {
                val inIdx = decoder.dequeueInputBuffer(10_000)
                if (inIdx >= 0) {
                    val inBuf = decoder.getInputBuffer(inIdx)!!
                    val size = extractor.readSampleData(inBuf, 0)
                    if (size < 0) {
                        decoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        decoder.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIdx = decoder.dequeueOutputBuffer(info, 10_000)
            when {
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val of = decoder.outputFormat
                    sampleRate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    resampler = AudioResampler(sampleRate, SAMPLE_RATE)
                }
                outIdx >= 0 -> {
                    val outBuf = decoder.getOutputBuffer(outIdx)!!
                    val shorts = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    val frames = shorts.remaining() / max(1, channels)
                    if (frames > 0) {
                        if (mono.size < frames) mono = FloatArray(frames)
                        if (channels == 1) {
                            for (i in 0 until frames) mono[i] = shorts.get(i) / 32768f
                        } else {
                            var m = 0
                            var s = 0
                            while (m < frames) {
                                var sum = 0
                                for (c in 0 until channels) sum += shorts.get(s++)
                                mono[m++] = (sum.toFloat() / channels) / 32768f
                            }
                        }
                        resampler.process(mono, 0, frames) { samples, count ->
                            chunker.push(samples, count, onChunk)
                        }
                    }
                    decoder.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        }
        resampler.flush { samples, count -> chunker.push(samples, count, onChunk) }
        chunker.finish(onChunk)
    } finally {
        runCatching { codec?.release() }
        extractor.release()
        runCatching { afd?.close() }
    }
}

/** Accumulates resampled samples and slices them into overlapping chunks. */
private class ChunkBuffer(private val chunkSamples: Int, private val stepSamples: Int) {
    private var buf = FloatArray(chunkSamples)
    private var size = 0
    private var index = 0

    fun push(
        input: FloatArray,
        count: Int,
        onChunk: (chunk: FloatArray, count: Int, chunkIndex: Int) -> Unit,
    ) {
        var offset = 0
        while (offset < count) {
            val copy = minOf(count - offset, chunkSamples - size)
            System.arraycopy(input, offset, buf, size, copy)
            size += copy
            offset += copy
            if (size == chunkSamples) {
                onChunk(buf, size, index++)
                // The next chunk re-starts [chunkSamples - stepSamples] before
                // this chunk's end, so carry that overlap over.
                val keep = chunkSamples - stepSamples
                System.arraycopy(buf, stepSamples, buf, 0, keep)
                size = keep
            }
        }
    }

    fun finish(onChunk: (chunk: FloatArray, count: Int, chunkIndex: Int) -> Unit) {
        val overlap = chunkSamples - stepSamples
        if (index == 0) {
            if (size > 0) onChunk(buf, size, index++)
        } else if (size > overlap) {
            // Chunk buffers already begin at the overlap boundary; anything
            // beyond the overlap is audio the previous chunk did not cover.
            onChunk(buf, size, index++)
        }
    }
}

actual suspend fun resolveAudioRealPath(path: String): String? = withIO {
    if (path.startsWith("/")) return@withIO path
    runCatching {
        appContext.contentResolver
            .query(path.pathToUri(), arrayOf(android.provider.MediaStore.MediaColumns.DATA), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }.getOrNull()?.takeIf { it.isNotEmpty() }
}

actual suspend fun findExistingLyricsFile(audioPath: String): String? = withIO {
    val real = resolveAudioRealPath(audioPath) ?: return@withIO null
    lyricsSiblingPath(real).takeIf { File(it).exists() }
}

actual suspend fun writeLyricsFile(audioPath: String, content: String): String? = withIO {
    val real = resolveAudioRealPath(audioPath) ?: return@withIO null
    val target = File(lyricsSiblingPath(real))
    runCatching { target.writeText(content) }.getOrElse { return@withIO null }
    target.absolutePath
}
