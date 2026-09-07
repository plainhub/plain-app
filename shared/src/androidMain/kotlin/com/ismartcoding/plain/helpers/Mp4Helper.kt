package com.ismartcoding.plain.helpers

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.ismartcoding.plain.lib.logcat.LogCat
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

/**
 * Convert 3gp (H.263 + AMR-NB) to mp4 (H.264 + AAC) for browser playback.
 *
 * Uses a three-phase approach to avoid MediaMuxer timing issues:
 *   Phase 1 – transcode/remux video into memory
 *   Phase 2 – transcode audio into memory
 *   Phase 3 – mux buffered samples into mp4 file
 */
object Mp4Helper {

    // Sample-entry fourccs Chromium's MP4 demuxer cannot identify: it fails the
    // whole file with DEMUXER_ERROR_COULD_NOT_OPEN instead of skipping the
    // track. "mebx" is the motion track Pixel cameras embed. Extend as new
    // offenders show up.
    private val BROWSER_INCOMPATIBLE_SAMPLE_ENTRIES = setOf("mebx")

    private class EncodedSample(
        val data: ByteArray,
        val presentationTimeUs: Long,
        val flags: Int,
    )

    private class TrackData(
        val format: MediaFormat,
        val samples: List<EncodedSample>,
    )

    /**
     * Media duration in seconds for fMP4 files whose moov reports
     * duration=0 (real duration lives in moof fragments), where
     * MediaMetadataRetriever fails. Two-tier strategy: MediaExtractor
     * (primary) + MP4 box parsing (fallback). The box parser matches the
     * video track, so audio-only fMP4 returns 0.
     */
    fun getMp4Duration(path: String): Long {
        val file = File(path)
        if (!file.exists() || file.length() < 8) return 0L

        val durationFromExtractor = getDurationViaExtractor(path)
        if (durationFromExtractor > 0) return durationFromExtractor

        val durationFromBoxes = getDurationFromMp4Boxes(path)
        if (durationFromBoxes > 0) return durationFromBoxes

        return 0L
    }

    private fun getDurationViaExtractor(path: String): Long {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(path)
            if (extractor.trackCount == 0) return 0L
            val format = extractor.getTrackFormat(0)
            if (!format.containsKey(MediaFormat.KEY_DURATION)) return 0L
            format.getLong(MediaFormat.KEY_DURATION) / 1_000_000L // microseconds → seconds
        } catch (e: Exception) {
            LogCat.e("getDurationViaExtractor failed: ${e.message}")
            0L
        } finally {
            try { extractor.release() } catch (_: Exception) {}
        }
    }

    private fun getDurationFromMp4Boxes(path: String): Long {
        return try {
            RandomAccessFile(path, "r").use { raf ->
                val fileLength = raf.length()
                var timescale = 0L
                var videoTrackId = -1

                // Phase 1: parse moov to get the video track ID and its mdhd timescale.
                // The video track is identified by an hdlr box whose handler_type == 'vide'.
                var offset = 0L
                while (offset in 0..<fileLength) {
                    raf.seek(offset)
                    val boxSize = readBoxSize(raf)
                    if (boxSize <= 0) break
                    val boxType = readBoxType(raf)

                    if (boxType == "moov") {
                        val moovEnd = offset + boxSize
                        var moovOffset = offset + 8
                        while (moovOffset < moovEnd) {
                            raf.seek(moovOffset)
                            val trakSize = readBoxSize(raf)
                            if (trakSize <= 0) break
                            val trakType = readBoxType(raf)

                            if (trakType == "trak") {
                                val trackInfo = parseTrakForVideoInfo(raf, moovOffset, trakSize)
                                if (trackInfo.isVideo && trackInfo.timescale > 0) {
                                    videoTrackId = trackInfo.trackId
                                    timescale = trackInfo.timescale
                                    break
                                }
                            }
                            moovOffset += trakSize
                        }
                        break
                    }
                    offset += boxSize
                }

                if (timescale <= 0) return@use 0L

                // Phase 2: sum trun sample durations across all moof fragments that
                // belong to the video track (matched via tfhd.track_ID).
                var totalDuration = 0L
                offset = 0L
                while (offset in 0..<fileLength) {
                    raf.seek(offset)
                    val boxSize = readBoxSize(raf)
                    if (boxSize <= 0) break
                    val boxType = readBoxType(raf)

                    if (boxType == "moof") {
                        totalDuration += parseMoofForVideoDuration(raf, offset, boxSize, videoTrackId)
                    }
                    offset += boxSize
                }

                if (totalDuration > 0) totalDuration / timescale else 0L
            }
        } catch (e: Exception) {
            LogCat.e("getDurationFromMp4Boxes failed: ${e.message}")
            0L
        }
    }

    private data class TrackInfo(
        val isVideo: Boolean,
        val trackId: Int,
        val timescale: Long,
    )

    /** Parse a trak box to determine if it's a video track, and extract its ID + timescale. */
    private fun parseTrakForVideoInfo(raf: RandomAccessFile, trakOffset: Long, trakSize: Long): TrackInfo {
        val trakEnd = trakOffset + trakSize
        var offset = trakOffset + 8
        var trackId = -1
        var isVideo = false
        var timescale = 0L

        while (offset < trakEnd) {
            raf.seek(offset)
            val subSize = readBoxSize(raf)
            if (subSize <= 0) break
            val subType = readBoxType(raf)

            when (subType) {
                "tkhd" -> {
                    raf.seek(offset + 8)
                    val version = raf.readByte().toInt()
                    raf.skipBytes(3) // flags
                    if (version == 0) {
                        raf.skipBytes(8) // creation + modification time
                    } else {
                        raf.skipBytes(16)
                    }
                    trackId = raf.readInt()
                }
                "mdia" -> {
                    val mdiaInfo = parseMdiaForVideoInfo(raf, offset, subSize)
                    if (mdiaInfo.isVideo) isVideo = true
                    if (mdiaInfo.timescale > 0) timescale = mdiaInfo.timescale
                }
            }
            offset += subSize
        }
        return TrackInfo(isVideo, trackId, timescale)
    }

    private data class MdiaInfo(val isVideo: Boolean, val timescale: Long)

    private fun parseMdiaForVideoInfo(raf: RandomAccessFile, mdiaOffset: Long, mdiaSize: Long): MdiaInfo {
        val mdiaEnd = mdiaOffset + mdiaSize
        var offset = mdiaOffset + 8
        var isVideo = false
        var timescale = 0L

        while (offset < mdiaEnd) {
            raf.seek(offset)
            val subSize = readBoxSize(raf)
            if (subSize <= 0) break
            val subType = readBoxType(raf)

            when (subType) {
                "mdhd" -> {
                    raf.seek(offset + 8)
                    val version = raf.readByte().toInt()
                    raf.skipBytes(3) // flags
                    if (version == 0) {
                        raf.skipBytes(8) // creation + modification time
                    } else {
                        raf.skipBytes(16)
                    }
                    timescale = raf.readInt().toLong() and 0xFFFFFFFFL
                }
                "hdlr" -> {
                    raf.seek(offset + 8)
                    raf.skipBytes(4) // version + flags
                    raf.skipBytes(4) // pre_defined
                    val handlerType = readBoxType(raf)
                    isVideo = handlerType == "vide"
                }
            }
            offset += subSize
        }
        return MdiaInfo(isVideo, timescale)
    }

    /** Parse a moof box: sum trun durations for the traf whose tfhd matches [videoTrackId]. */
    private fun parseMoofForVideoDuration(
        raf: RandomAccessFile,
        moofOffset: Long,
        moofSize: Long,
        videoTrackId: Int,
    ): Long {
        val moofEnd = moofOffset + moofSize
        var offset = moofOffset + 8
        var totalDuration = 0L

        while (offset < moofEnd) {
            raf.seek(offset)
            val subSize = readBoxSize(raf)
            if (subSize <= 0) break
            val subType = readBoxType(raf)

            if (subType == "traf") {
                val trafInfo = parseTrafForVideoTrun(raf, offset, subSize, videoTrackId)
                totalDuration += trafInfo
            }
            offset += subSize
        }
        return totalDuration
    }

    /** Parse traf: read tfhd to get track_ID and defaultSampleDuration, and if
     *  track_ID matches, sum trun sample durations. Per-sample durations come
     *  from the trun when sampleDurationPresent is set; otherwise each sample
     *  uses tfhd.defaultSampleDuration (common for fMP4 produced by libavformat
     *  / Pixel camera where the trun omits per-sample durations). */
    private fun parseTrafForVideoTrun(
        raf: RandomAccessFile,
        trafOffset: Long,
        trafSize: Long,
        videoTrackId: Int,
    ): Long {
        val trafEnd = trafOffset + trafSize
        var offset = trafOffset + 8
        var trafTrackId = -1
        var defaultSampleDuration = 0L
        var trunDuration = 0L
        var foundTrun = false

        // First pass: find tfhd to get track_ID and defaultSampleDuration
        while (offset < trafEnd) {
            raf.seek(offset)
            val subSize = readBoxSize(raf)
            if (subSize <= 0) break
            val subType = readBoxType(raf)

            if (subType == "tfhd") {
                raf.seek(offset + 8)
                val flags = (raf.readByte().toInt() and 0xFF) shl 24 or
                    (raf.readByte().toInt() and 0xFF) shl 16 or
                    (raf.readByte().toInt() and 0xFF) shl 8 or
                    (raf.readByte().toInt() and 0xFF)
                trafTrackId = raf.readInt()
                // defaultSampleDurationPresent (flag 0x08) → 4 bytes
                if ((flags and 0x08) != 0) {
                    defaultSampleDuration = raf.readInt().toLong() and 0xFFFFFFFFL
                }
                break
            }
            offset += subSize
        }

        // If this traf is not for the video track, skip it
        if (videoTrackId >= 0 && trafTrackId != videoTrackId) return 0L

        // Second pass: sum trun durations
        offset = trafOffset + 8
        while (offset < trafEnd) {
            raf.seek(offset)
            val subSize = readBoxSize(raf)
            if (subSize <= 0) break
            val subType = readBoxType(raf)

            if (subType == "trun") {
                trunDuration += parseTrunDuration(raf, offset, subSize, defaultSampleDuration)
                foundTrun = true
            }
            offset += subSize
        }
        return if (foundTrun) trunDuration else 0L
    }

    /** Parse a trun box and return the sum of per-sample durations (in timescale
     *  units). When sampleDurationPresent is false, each sample uses
     *  [defaultSampleDuration] from tfhd. */
    private fun parseTrunDuration(
        raf: RandomAccessFile,
        trunOffset: Long,
        trunSize: Long,
        defaultSampleDuration: Long,
    ): Long {
        raf.seek(trunOffset + 8)
        val version = raf.readByte().toInt()
        val flags = (raf.readByte().toInt() and 0xFF) shl 16 or
            (raf.readByte().toInt() and 0xFF) shl 8 or
            (raf.readByte().toInt() and 0xFF)

        val dataOffsetPresent = (flags and 0x000001) != 0
        val firstSampleFlagsPresent = (flags and 0x000004) != 0
        val sampleDurationPresent = (flags and 0x000100) != 0
        val sampleSizePresent = (flags and 0x000200) != 0
        val sampleFlagsPresent = (flags and 0x000400) != 0
        val sampleCompositionOffsetsPresent = (flags and 0x000800) != 0

        val sampleCount = raf.readInt().toLong() and 0xFFFFFFFFL
        if (sampleCount == 0L) return 0L

        // data_offset (optional)
        if (dataOffsetPresent) raf.skipBytes(4)
        // first_sample_flags (optional) — read BEFORE the samples array
        if (firstSampleFlagsPresent) raf.skipBytes(4)

        var totalDuration = 0L
        var remaining = sampleCount
        while (remaining > 0) {
            if (sampleDurationPresent) {
                totalDuration += raf.readInt().toLong() and 0xFFFFFFFFL
            } else {
                totalDuration += defaultSampleDuration
            }
            if (sampleSizePresent) raf.skipBytes(4)
            if (sampleFlagsPresent) raf.skipBytes(4)
            if (sampleCompositionOffsetsPresent) raf.skipBytes(4)
            remaining--
        }
        return totalDuration
    }

    private fun readBoxSize(raf: RandomAccessFile): Long {
        val size = raf.readInt().toLong() and 0xFFFFFFFFL
        return when {
            size == 0L -> -1L  // box extends to end of file; signal to stop
            size == 1L -> raf.readLong()
            else -> size
        }
    }

    private fun readBoxType(raf: RandomAccessFile): String {
        val bytes = ByteArray(4)
        raf.readFully(bytes)
        return String(bytes, Charsets.US_ASCII)
    }

    fun convert3gpToMp4(context: Context, uri: Uri): ByteArray? {
        val tmpFile = File.createTempFile("mms_", ".mp4", context.cacheDir)
        try {
            // ── Discover tracks ──────────────────────────────────────────
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null

            MediaExtractor().also { ext ->
                ext.setDataSource(context, uri, null)
                for (i in 0 until ext.trackCount) {
                    val fmt = ext.getTrackFormat(i)
                    val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/") && videoTrackIndex < 0) {
                        videoTrackIndex = i; videoFormat = fmt
                    } else if (mime.startsWith("audio/") && audioTrackIndex < 0) {
                        audioTrackIndex = i; audioFormat = fmt
                    }
                }
                ext.release()
            }

            if (videoTrackIndex < 0 || videoFormat == null) {
                LogCat.e("Mp4Helper: no video track found")
                return null
            }

            val videoMime = videoFormat.getString(MediaFormat.KEY_MIME)!!
            val isH264 = videoMime == MediaFormat.MIMETYPE_VIDEO_AVC
            LogCat.d("Mp4Helper: video=$videoMime ${if (isH264) "(remux)" else "(transcode)"}, audio=${audioFormat?.getString(MediaFormat.KEY_MIME) ?: "none"}")

            // ── Phase 1: process video ───────────────────────────────────
            val videoData: TrackData = run {
                val ext = MediaExtractor()
                ext.setDataSource(context, uri, null)
                ext.selectTrack(videoTrackIndex)
                try {
                    if (isH264) remuxTrack(ext, videoFormat) else transcodeH263ToH264(ext, videoFormat!!)
                } finally {
                    ext.release()
                }
            }

            // ── Phase 2: process audio ───────────────────────────────────
            val audioData: TrackData? = if (audioTrackIndex >= 0 && audioFormat != null) {
                val ext = MediaExtractor()
                ext.setDataSource(context, uri, null)
                ext.selectTrack(audioTrackIndex)
                try {
                    transcodeAmrToAac(ext, audioFormat!!)
                } finally {
                    ext.release()
                }
            } else null

            // ── Phase 3: mux into mp4 (interleaved by timestamp) ─────────
            LogCat.d("Mp4Helper: muxing video=${videoData.samples.size} frames, audio=${audioData?.samples?.size ?: 0} frames")

            val muxer = MediaMuxer(tmpFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val videoOutTrack = muxer.addTrack(videoData.format)
            val audioOutTrack = audioData?.let { muxer.addTrack(it.format) } ?: -1
            muxer.start()

            val buf = ByteBuffer.allocate(1024 * 1024)
            val info = MediaCodec.BufferInfo()

            // Merge video and audio samples, interleaved by presentation time.
            // This matches MPEG4Writer's expected interleaving pattern and avoids
            // writer-thread race conditions that can stop all tracks prematurely.
            data class MuxSample(val trackIndex: Int, val sample: EncodedSample)

            val merged = mutableListOf<MuxSample>()
            for (s in videoData.samples) merged.add(MuxSample(videoOutTrack, s))
            if (audioData != null && audioOutTrack >= 0) {
                for (s in audioData.samples) merged.add(MuxSample(audioOutTrack, s))
            }
            merged.sortBy { it.sample.presentationTimeUs }

            for (ms in merged) {
                buf.clear(); buf.put(ms.sample.data); buf.flip()
                // Only keep KEY_FRAME flag; strip CODEC_CONFIG, EOS, and others
                val cleanFlags = if (ms.sample.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0) {
                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else {
                    0
                }
                info.set(0, ms.sample.data.size, ms.sample.presentationTimeUs, cleanFlags)
                muxer.writeSampleData(ms.trackIndex, buf, info)
            }

            muxer.stop()
            muxer.release()

            LogCat.d("Mp4Helper: output ${tmpFile.length()} bytes, ${merged.size} total samples")
            return tmpFile.readBytes()
        } catch (e: Exception) {
            e.printStackTrace()
            LogCat.e(e)
            return null
        } finally {
            tmpFile.delete()
        }
    }

    // ── remux: copy compressed samples as-is ─────────────────────────────

    private fun remuxTrack(extractor: MediaExtractor, format: MediaFormat): TrackData {
        val samples = mutableListOf<EncodedSample>()
        val buffer = ByteBuffer.allocate(1024 * 1024)
        while (true) {
            buffer.clear()
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            val data = ByteArray(size)
            buffer.flip()
            buffer.get(data)
            samples.add(EncodedSample(data, extractor.sampleTime, extractor.sampleFlags))
            extractor.advance()
        }
        return TrackData(format, samples)
    }

    // ── H.263 → H.264  (decode → surface → encode) ──────────────────────

    private fun transcodeH263ToH264(extractor: MediaExtractor, inputFormat: MediaFormat): TrackData {
        val width = inputFormat.getInteger(MediaFormat.KEY_WIDTH)
        val height = inputFormat.getInteger(MediaFormat.KEY_HEIGHT)
        val frameRate = try { inputFormat.getInteger(MediaFormat.KEY_FRAME_RATE) } catch (_: Exception) { 15 }

        // Encoder
        val encFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 500_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(encFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = encoder.createInputSurface()
        encoder.start()

        // Decoder → renders onto encoder's input surface
        val decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME)!!)
        decoder.configure(inputFormat, inputSurface, null, 0)
        decoder.start()

        val samples = mutableListOf<EncodedSample>()
        var outputFormat: MediaFormat? = null
        val bufferInfo = MediaCodec.BufferInfo()
        var decInputEOS = false
        var decOutputEOS = false
        var encOutputEOS = false

        while (!encOutputEOS) {
            // 1. Feed compressed H.263 into decoder
            if (!decInputEOS) {
                val idx = decoder.dequeueInputBuffer(10_000)
                if (idx >= 0) {
                    val buf = decoder.getInputBuffer(idx)!!
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0) {
                        decoder.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        decInputEOS = true
                    } else {
                        decoder.queueInputBuffer(idx, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            // 2. Drain decoder output → rendered to surface automatically
            if (!decOutputEOS) {
                val idx = decoder.dequeueOutputBuffer(bufferInfo, 10_000)
                if (idx >= 0) {
                    decoder.releaseOutputBuffer(idx, bufferInfo.size > 0)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        decOutputEOS = true
                        encoder.signalEndOfInputStream()
                    }
                }
            }

            // 3. Drain encoder output → collect H.264 samples
            val idx = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
            if (idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                outputFormat = encoder.outputFormat
                LogCat.d("Mp4Helper: H.264 encoder output format: $outputFormat")
            } else if (idx >= 0) {
                val isConfig = bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                if (bufferInfo.size > 0 && !isConfig) {
                    val encBuf = encoder.getOutputBuffer(idx)!!
                    val data = ByteArray(bufferInfo.size)
                    encBuf.position(bufferInfo.offset)
                    encBuf.get(data)
                    samples.add(EncodedSample(data, bufferInfo.presentationTimeUs, bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME))
                }
                encoder.releaseOutputBuffer(idx, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    encOutputEOS = true
                }
            }
        }

        decoder.stop(); decoder.release()
        encoder.stop(); encoder.release()
        inputSurface.release()

        LogCat.d("Mp4Helper: H.264 transcode done, ${samples.size} frames, format=$outputFormat")
        return TrackData(outputFormat ?: encFormat, samples)
    }

    // ── AMR-NB → AAC  (decode → PCM → encode) ───────────────────────────

    private fun transcodeAmrToAac(extractor: MediaExtractor, inputFormat: MediaFormat): TrackData {
        val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val audioMime = inputFormat.getString(MediaFormat.KEY_MIME)!!

        // Decoder
        val decoder = MediaCodec.createDecoderByType(audioMime)
        decoder.configure(inputFormat, null, null, 0)
        decoder.start()

        // Encoder
        val aacFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 64_000)
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder.configure(aacFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val samples = mutableListOf<EncodedSample>()
        var outputFormat: MediaFormat? = null
        val bufferInfo = MediaCodec.BufferInfo()
        var decInputEOS = false
        var encOutputEOS = false

        while (!encOutputEOS) {
            // 1. Feed AMR into decoder
            if (!decInputEOS) {
                val idx = decoder.dequeueInputBuffer(10_000)
                if (idx >= 0) {
                    val buf = decoder.getInputBuffer(idx)!!
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0) {
                        decoder.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        decInputEOS = true
                    } else {
                        decoder.queueInputBuffer(idx, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            // 2. Drain decoder → feed encoder
            val decIdx = decoder.dequeueOutputBuffer(bufferInfo, 10_000)
            if (decIdx >= 0) {
                val pcm = decoder.getOutputBuffer(decIdx)!!
                val encInIdx = encoder.dequeueInputBuffer(10_000)
                if (encInIdx >= 0) {
                    val encBuf = encoder.getInputBuffer(encInIdx)!!
                    encBuf.clear()
                    encBuf.put(pcm)
                    encoder.queueInputBuffer(encInIdx, 0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags)
                }
                decoder.releaseOutputBuffer(decIdx, false)
            }

            // 3. Drain encoder → collect AAC samples
            val encIdx = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
            if (encIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                outputFormat = encoder.outputFormat
                LogCat.d("Mp4Helper: AAC encoder output format: $outputFormat")
            } else if (encIdx >= 0) {
                val isConfig = bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                if (bufferInfo.size > 0 && !isConfig) {
                    val encBuf = encoder.getOutputBuffer(encIdx)!!
                    val data = ByteArray(bufferInfo.size)
                    encBuf.position(bufferInfo.offset)
                    encBuf.get(data)
                    samples.add(EncodedSample(data, bufferInfo.presentationTimeUs, bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME))
                }
                encoder.releaseOutputBuffer(encIdx, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    encOutputEOS = true
                }
            }
        }

        decoder.stop(); decoder.release()
        encoder.stop(); encoder.release()

        LogCat.d("Mp4Helper: AAC transcode done, ${samples.size} frames, format=$outputFormat")
        return TrackData(outputFormat ?: aacFormat, samples)
    }

    // ── Browser-compat remux: drop undecodable metadata tracks ───────────

    /**
     * True when the MP4's moov declares a track whose stsd sample entry is one
     * browsers refuse to demux ([BROWSER_INCOMPATIBLE_SAMPLE_ENTRIES]). Box-level
     * probe only — no MediaExtractor, safe to call on the request path.
     */
    fun hasBrowserIncompatibleTrack(path: String): Boolean {
        val file = File(path)
        if (file.length() < 32) return false
        return try {
            RandomAccessFile(path, "r").use { raf ->
                val length = file.length()
                var offset = 0L
                while (offset < length) {
                    val box = readBoxHeader(raf, offset, length) ?: return@use false
                    if (box.type == "moov") {
                        return@use moovHasIncompatibleTrack(raf, box)
                    }
                    offset += box.size
                }
                false
            }
        } catch (e: Exception) {
            LogCat.e("hasBrowserIncompatibleTrack failed: ${e.message}")
            false
        }
    }

    private fun moovHasIncompatibleTrack(raf: RandomAccessFile, moov: BoxRef): Boolean {
        var offset = moov.childrenStart
        while (offset < moov.end) {
            val box = readBoxHeader(raf, offset, moov.end) ?: return false
            if (box.type == "trak") {
                val entry = firstSampleEntry(raf, box)
                if (entry != null && entry in BROWSER_INCOMPATIBLE_SAMPLE_ENTRIES) return true
            }
            offset += box.size
        }
        return false
    }

    /**
     * fourcc of the first video track's stsd sample entry (e.g. "hvc1", "avc1"),
     * or null when no video track is found. Box-level probe only, safe on the
     * request path. Used by `/fs` to let the browser decide whether it can
     * decode the file before committing to a playback URL.
     */
    fun firstVideoSampleEntry(path: String): String? {
        val file = File(path)
        if (file.length() < 32) return null
        return try {
            RandomAccessFile(path, "r").use { raf ->
                val length = file.length()
                var offset = 0L
                while (offset < length) {
                    val box = readBoxHeader(raf, offset, length) ?: return@use null
                    if (box.type == "moov") {
                        return@use moovFirstVideoSampleEntry(raf, box)
                    }
                    offset += box.size
                }
                null
            }
        } catch (e: Exception) {
            LogCat.e("firstVideoSampleEntry failed: ${e.message}")
            null
        }
    }

    private fun moovFirstVideoSampleEntry(raf: RandomAccessFile, moov: BoxRef): String? {
        var offset = moov.childrenStart
        while (offset < moov.end) {
            val box = readBoxHeader(raf, offset, moov.end) ?: return null
            if (box.type == "trak" && trakIsVideo(raf, box)) {
                return firstSampleEntry(raf, box)
            }
            offset += box.size
        }
        return null
    }

    /** True when the trak's mdia/hdlr declares a "vide" handler. */
    private fun trakIsVideo(raf: RandomAccessFile, trak: BoxRef): Boolean {
        val mdia = findChildBox(raf, trak, "mdia") ?: return false
        val hdlr = findChildBox(raf, mdia, "hdlr") ?: return false
        if (hdlr.end - hdlr.childrenStart < 12) return false
        raf.seek(hdlr.childrenStart + 8) // version+flags (4) + pre_defined (4)
        return readBoxType(raf) == "vide"
    }

    /** fourcc of the first stsd sample entry inside [trak], or null. */
    private fun firstSampleEntry(raf: RandomAccessFile, trak: BoxRef): String? {
        val mdia = findChildBox(raf, trak, "mdia") ?: return null
        val minf = findChildBox(raf, mdia, "minf") ?: return null
        val stbl = findChildBox(raf, minf, "stbl") ?: return null
        val stsd = findChildBox(raf, stbl, "stsd") ?: return null
        // stsd payload: version+flags (4) + entry_count (4), then sample entries.
        if (stsd.end - stsd.childrenStart < 16) return null
        raf.seek(stsd.childrenStart + 8)
        val entrySize = raf.readInt().toLong() and 0xFFFFFFFFL
        if (entrySize < 8) return null
        return readBoxType(raf)
    }

    private fun findChildBox(raf: RandomAccessFile, parent: BoxRef, type: String): BoxRef? {
        var offset = parent.childrenStart
        while (offset < parent.end) {
            val box = readBoxHeader(raf, offset, parent.end) ?: return null
            if (box.type == type) return box
            offset += box.size
        }
        return null
    }

    private fun readBoxHeader(raf: RandomAccessFile, offset: Long, limit: Long): BoxRef? {
        if (offset + 8 > limit) return null
        raf.seek(offset)
        var size = raf.readInt().toLong() and 0xFFFFFFFFL
        val type = readBoxType(raf)
        var headerLen = 8
        if (size == 1L) {
            if (offset + 16 > limit) return null
            size = raf.readLong()
            headerLen = 16
        } else if (size == 0L) {
            size = limit - offset
        }
        if (size < headerLen || offset + size > limit) return null
        return BoxRef(type, offset, size, headerLen)
    }

    private class BoxRef(val type: String, val start: Long, val size: Long, val headerLen: Int) {
        val childrenStart: Long get() = start + headerLen
        val end: Long get() = start + size
    }

    /**
     * Stream-copy remux that keeps video/audio tracks only and drops
     * undecodable metadata tracks, so Chromium-based browsers can demux the
     * file. Result is cached under cacheDir/remux keyed by path+size+mtime.
     * Returns the cached file path, or null when nothing needs stripping /
     * the remux fails (caller should serve the original bytes).
     */
    fun remuxForBrowser(context: Context, path: String): String? {
        val src = File(path)
        if (!src.exists() || src.length() == 0L) return null
        val cacheDir = File(context.cacheDir, "remux").apply { mkdirs() }
        val cacheFile = File(
            cacheDir,
            "web_${src.absolutePath.hashCode().toUInt()}_${src.length()}_${src.lastModified()}.mp4",
        )
        if (cacheFile.exists() && cacheFile.length() > 0) return cacheFile.absolutePath

        val tmpFile = File(cacheDir, "tmp_${System.nanoTime()}.mp4")
        try {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(path)
                val outTracks = HashMap<Int, Int>()
                var rotationDegrees = 0
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    val isMedia = mime != null &&
                        (mime.startsWith("video/") || mime.startsWith("audio/"))
                    if (!isMedia) continue
                    if (mime!!.startsWith("video/") &&
                        format.containsKey(MediaFormat.KEY_ROTATION)
                    ) {
                        rotationDegrees = format.getInteger(MediaFormat.KEY_ROTATION)
                    }
                    outTracks[i] = 0
                }
                // Extractor exposes media tracks only — no stripping possible.
                if (outTracks.isEmpty() || outTracks.size == extractor.trackCount) return null

                val muxer = MediaMuxer(tmpFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                var started = false
                try {
                    if (rotationDegrees != 0) muxer.setOrientationHint(rotationDegrees)
                    for (i in outTracks.keys) {
                        outTracks[i] = muxer.addTrack(extractor.getTrackFormat(i))
                    }
                    for (i in outTracks.keys) extractor.selectTrack(i)
                    muxer.start()
                    started = true
                    // readSampleData() advances selected tracks in pts order,
                    // so samples interleave naturally with O(1) memory.
                    val buffer = ByteBuffer.allocate(4 * 1024 * 1024)
                    val info = MediaCodec.BufferInfo()
                    while (true) {
                        buffer.clear()
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) break
                        val target = outTracks[extractor.sampleTrackIndex]
                        if (target != null) {
                            // Map extractor flag domain → MediaCodec flag domain
                            // (same bit for sync/key frames, but the BufferInfo
                            // contract only allows MediaCodec constants).
                            info.set(
                                0, size, extractor.sampleTime,
                                if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0)
                                    MediaCodec.BUFFER_FLAG_KEY_FRAME else 0,
                            )
                            muxer.writeSampleData(target, buffer, info)
                        }
                        extractor.advance()
                    }
                    muxer.stop()
                    started = false
                } finally {
                    if (started) {
                        try { muxer.stop() } catch (_: Exception) {}
                    }
                    muxer.release()
                }
            } finally {
                try { extractor.release() } catch (_: Exception) {}
            }

            if (!tmpFile.renameTo(cacheFile) || cacheFile.length() == 0L) {
                LogCat.e("Mp4Helper: remuxForBrowser produced no output")
                return null
            }
            LogCat.d("Mp4Helper: remuxForBrowser $path -> ${cacheFile.name}")
            return cacheFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            LogCat.e(e)
            return null
        } finally {
            tmpFile.delete()
        }
    }

    // ── Browser-compat transcode: HEVC → H.264 for browsers without HEVC ──

    private val transcodeLock = Any()
    private val HEVC_SAMPLE_ENTRIES = setOf("hvc1", "hev1")
    // Waiting on a 10-minute transcode over HTTP is worse than downloading;
    // longer videos fall back to the "download to watch" hint in the web UI.
    private const val MAX_TRANSCODE_DURATION_SECONDS = 600L

    fun isHevc(path: String): Boolean = firstVideoSampleEntry(path) in HEVC_SAMPLE_ENTRIES

    private class PendingSample(val data: ByteArray, val pts: Long, val flags: Int)

    /**
     * Transcode the HEVC video track to H.264 (hardware decode → hardware
     * encode through a Surface, audio stream-copied) so browsers without an
     * HEVC decoder can play the file. One transcode at a time process-wide —
     * MediaCodec instances are heavy. Result is cached next to the remux
     * cache; returns null when the file is not HEVC / too long / the
     * pipeline fails (caller serves an error or the original bytes).
     */
    fun transcodeForBrowser(context: Context, path: String): String? = synchronized(transcodeLock) {
        val src = File(path)
        if (!src.exists() || src.length() == 0L) return null
        if (!isHevc(path)) return null
        val duration = getMp4Duration(path)
        if (duration > MAX_TRANSCODE_DURATION_SECONDS) {
            LogCat.d("Mp4Helper: transcode skipped, duration ${duration}s > ${MAX_TRANSCODE_DURATION_SECONDS}s")
            return null
        }

        val cacheDir = File(context.cacheDir, "remux").apply { mkdirs() }
        val cacheFile = File(
            cacheDir,
            "webtc_${src.absolutePath.hashCode().toUInt()}_${src.length()}_${src.lastModified()}.mp4",
        )
        if (cacheFile.exists() && cacheFile.length() > 0) return cacheFile.absolutePath

        val tmpFile = File(cacheDir, "tmp_${System.nanoTime()}.mp4")
        try {
            transcodeToFile(path, tmpFile)
            if (!tmpFile.renameTo(cacheFile) || cacheFile.length() == 0L) {
                LogCat.e("Mp4Helper: transcodeForBrowser produced no output")
                return null
            }
            LogCat.d("Mp4Helper: transcodeForBrowser $path -> ${cacheFile.name} (${cacheFile.length()} bytes)")
            cacheFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            LogCat.e(e)
            null
        } finally {
            tmpFile.delete()
        }
    }

    /**
     * Single-pass interleaved pipeline: one extractor feeds both selected
     * tracks in presentation order — video samples go through the
     * decoder→encoder Surface path, audio samples are muxed as-is. The
     * encoder lags its input by a few frames of codec latency, which
     * MediaMuxer absorbs with its per-track chunk buffering.
     */
    private fun transcodeToFile(path: String, outFile: File) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var inputSurface: android.view.Surface? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        try {
            extractor.setDataSource(path)

            var videoTrack = -1
            var audioTrack = -1
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null
            var rotationDegrees = 0
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") && videoTrack < 0) {
                    videoTrack = i
                    videoFormat = format
                    if (format.containsKey(MediaFormat.KEY_ROTATION)) {
                        rotationDegrees = format.getInteger(MediaFormat.KEY_ROTATION)
                    }
                } else if (mime.startsWith("audio/") && audioTrack < 0) {
                    audioTrack = i
                    audioFormat = format
                }
            }
            if (videoTrack < 0 || videoFormat == null) throw IllegalStateException("no video track")
            extractor.selectTrack(videoTrack)
            if (audioTrack >= 0) extractor.selectTrack(audioTrack)

            // H.264 encoder: bitrate ≈ 0.15 bits per pixel per frame keeps
            // 1080p30 camera content visually clean (~9 Mbps) and scales to 4K.
            val width = videoFormat.getInteger(MediaFormat.KEY_WIDTH)
            val height = videoFormat.getInteger(MediaFormat.KEY_HEIGHT)
            val frameRate = try { videoFormat.getInteger(MediaFormat.KEY_FRAME_RATE) } catch (_: Exception) { 30 }
            val bitrate = (width.toLong() * height * frameRate * 3 / 20)
                .coerceIn(4_000_000L, 50_000_000L).toInt()
            val encFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                // Shallow the encoder pipeline so the sync drain loop keeps
                // the hardware fed (ignored by encoders that don't support it).
                setInteger(MediaFormat.KEY_LATENCY, 1)
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            }
            encoder = try {
                MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).also { enc ->
                    encFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
                    enc.configure(encFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                }
            } catch (e: Exception) {
                // Some encoders reject an explicit profile; plain configure is Baseline.
                LogCat.d("Mp4Helper: High profile rejected (${e.message}), retrying default")
                MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).also { enc ->
                    enc.configure(encFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                }
            }
            inputSurface = encoder.createInputSurface()
            encoder.start()

            decoder = MediaCodec.createDecoderByType(videoFormat.getString(MediaFormat.KEY_MIME)!!).also { dec ->
                dec.configure(videoFormat, inputSurface, null, 0)
                dec.start()
            }

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            if (rotationDegrees != 0) muxer.setOrientationHint(rotationDegrees)
            val audioOut = audioFormat?.let { muxer.addTrack(it) } ?: -1
            var videoOut = -1
            var muxFormatChanged = false
            val pendingAudio = ArrayDeque<PendingSample>()

            val info = MediaCodec.BufferInfo()
            val audioBuffer = ByteBuffer.allocate(512 * 1024)
            var extractorEOS = false
            var videoEosQueued = false
            var decEOS = false
            var encEOS = false

            // Throughput contract: each pass feeds every free decoder input
            // slot and drains both codecs with 0 µs timeouts (pure
            // non-blocking sweeps), so hardware buffers never sit idle while
            // the loop blocks on a single 10 ms dequeue. A blocking wait
            // happens only when a pass made no progress at all, and only at
            // the pipeline's end stage (encoder output).
            fun feedInputs(): Boolean {
                var progressed = false
                while (!extractorEOS) {
                    val trackIdx = extractor.sampleTrackIndex
                    if (trackIdx == videoTrack) {
                        val idx = decoder.dequeueInputBuffer(0)
                        if (idx < 0) return progressed // input queue full — drain first
                        val buf = decoder.getInputBuffer(idx)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            videoEosQueued = true
                            extractorEOS = true
                            return progressed
                        }
                        decoder.queueInputBuffer(idx, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                        progressed = true
                    } else {
                        audioBuffer.clear()
                        val size = extractor.readSampleData(audioBuffer, 0)
                        if (size < 0) {
                            // readSampleData()==-1 (both tracks exhausted) is
                            // the only EOS oracle — sampleTrackIndex is -1
                            // here, so the video EOS must be queued from this
                            // branch if the video path never saw it.
                            if (videoEosQueued) {
                                extractorEOS = true
                                return progressed
                            }
                            val idx = decoder.dequeueInputBuffer(0)
                            if (idx < 0) return progressed // busy — retry next pass
                            decoder.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            videoEosQueued = true
                            extractorEOS = true
                            return progressed
                        }
                        val flags =
                            if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0)
                                MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                        if (trackIdx == audioTrack) {
                            // The muxer can only start once the encoder
                            // announces its output format (a few video frames
                            // in), while MP4 interleaving puts audio among the
                            // very first samples — buffer audio until then
                            // instead of writing into an unstarted muxer
                            // (IllegalStateException).
                            if (muxerStarted) {
                                info.set(0, size, extractor.sampleTime, flags)
                                muxer.writeSampleData(audioOut, audioBuffer, info)
                            } else {
                                val data = ByteArray(size)
                                audioBuffer.position(0)
                                audioBuffer.get(data)
                                pendingAudio.add(PendingSample(data, extractor.sampleTime, flags))
                            }
                        }
                        // Audio or unselected metadata track — consume it.
                        extractor.advance()
                        progressed = true
                    }
                }
                return progressed
            }

            fun drainDecoder(): Boolean {
                if (decEOS) return false
                var progressed = false
                while (true) {
                    val idx = decoder.dequeueOutputBuffer(info, 0)
                    if (idx == MediaCodec.INFO_TRY_AGAIN_LATER) return progressed
                    if (idx >= 0) {
                        decoder.releaseOutputBuffer(idx, info.size > 0)
                        progressed = true
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            decEOS = true
                            encoder.signalEndOfInputStream()
                            return progressed
                        }
                    }
                }
            }

            fun drainEncoder(timeoutUs: Long): Boolean {
                var progressed = false
                var blockLeft = timeoutUs
                while (!encEOS) {
                    val idx = encoder.dequeueOutputBuffer(info, blockLeft)
                    blockLeft = 0 // only the first dequeue may block
                    when {
                        idx == MediaCodec.INFO_TRY_AGAIN_LATER -> return progressed
                        idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            videoOut = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                            muxFormatChanged = true
                            // Flush the audio that led the video in
                            // presentation order before any video sample is
                            // written.
                            while (pendingAudio.isNotEmpty()) {
                                val p = pendingAudio.removeFirst()
                                info.set(0, p.data.size, p.pts, p.flags)
                                muxer.writeSampleData(audioOut, ByteBuffer.wrap(p.data), info)
                            }
                            progressed = true
                        }
                        idx >= 0 -> {
                            if (!muxFormatChanged) throw IllegalStateException("encoder produced samples before format")
                            val isConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                            if (info.size > 0 && !isConfig) {
                                muxer.writeSampleData(videoOut, encoder.getOutputBuffer(idx)!!, info)
                            }
                            encoder.releaseOutputBuffer(idx, false)
                            progressed = true
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                encEOS = true
                            }
                        }
                    }
                }
                return progressed
            }

            while (!encEOS) {
                var progressed = feedInputs()
                progressed = drainDecoder() || progressed
                progressed = drainEncoder(0) || progressed
                if (!progressed && !encEOS) {
                    drainEncoder(10_000)
                }
            }

            if (!muxerStarted) throw IllegalStateException("encoder emitted no output")
            muxer.stop()
            muxerStarted = false
        } finally {
            if (muxerStarted) try { muxer?.stop() } catch (_: Exception) {}
            runCatching { muxer?.release() }
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { encoder?.stop() }
            runCatching { encoder?.release() }
            runCatching { inputSurface?.release() }
            runCatching { extractor.release() }
        }
    }
}