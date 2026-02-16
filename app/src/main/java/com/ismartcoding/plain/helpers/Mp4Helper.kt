package com.ismartcoding.plain.helpers

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.ismartcoding.lib.logcat.LogCat
import java.io.File
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

    private class EncodedSample(
        val data: ByteArray,
        val presentationTimeUs: Long,
        val flags: Int,
    )

    private class TrackData(
        val format: MediaFormat,
        val samples: List<EncodedSample>,
    )

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

            val videoMime = videoFormat!!.getString(MediaFormat.KEY_MIME)!!
            val isH264 = videoMime == MediaFormat.MIMETYPE_VIDEO_AVC
            LogCat.d("Mp4Helper: video=$videoMime ${if (isH264) "(remux)" else "(transcode)"}, audio=${audioFormat?.getString(MediaFormat.KEY_MIME) ?: "none"}")

            // ── Phase 1: process video ───────────────────────────────────
            val videoData: TrackData = run {
                val ext = MediaExtractor()
                ext.setDataSource(context, uri, null)
                ext.selectTrack(videoTrackIndex)
                try {
                    if (isH264) remuxTrack(ext, videoFormat!!) else transcodeH263ToH264(ext, videoFormat!!)
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
}