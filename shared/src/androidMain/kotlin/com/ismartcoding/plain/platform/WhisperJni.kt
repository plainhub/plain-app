package com.ismartcoding.plain.platform

/**
 * Thin JNI surface over the vendored whisper.cpp build (app/src/main/cpp/).
 * Class and method names are bound to JNI symbols — keep them stable and
 * preserved by R8 (see app/proguard-rules.pro).
 */
object WhisperJni {
    init {
        System.loadLibrary("whisper_jni")
    }

    /** Segment produced by whisper_full. [t0]/[t1] are centiseconds; [text] is raw UTF-8 bytes. */
    class Segment(val t0: Long, val t1: Long, val text: ByteArray)

    fun interface ProgressListener {
        /** Return true to abort the current transcription. */
        fun onProgress(percent: Int): Boolean
    }

    /** Loads a ggml model file and returns an opaque context handle (0 on failure). */
    external fun nativeLoadModel(path: String): Long

    external fun nativeFreeModel(handle: Long)

    /**
     * Runs transcription over a 16 kHz mono float PCM chunk. Returns segments
     * or null on failure / cancellation (via [ProgressListener]).
     */
    external fun nativeRun(
        handle: Long,
        pcm: FloatArray,
        pcmLength: Int,
        language: String,
        threads: Int,
        progressListener: ProgressListener?,
    ): Array<Segment>?
}
