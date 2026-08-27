package com.ismartcoding.plain.ai

import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.TensorBuffer
import com.ismartcoding.plain.lib.logcat.LogCat
import java.io.File

object TextEmbedHelper {
    private var model: CompiledModel? = null
    private var tokenizer: CLIPTokenizer? = null
    private var inputBuffers: List<TensorBuffer>? = null
    private var outputBuffers: List<TensorBuffer>? = null
    private var modelFile: File? = null
    private var tokenizerFile: File? = null

    // Guards init/release against concurrent embed calls (IO threads vs onTrimMemory).
    private val lock = Any()

    fun init(modelFile: File, tokenizerFile: File) {
        synchronized(lock) {
            close()
            this.modelFile = modelFile
            this.tokenizerFile = tokenizerFile
            if (!loadLocked()) {
                throw IllegalStateException("TextEmbedHelper: model init failed")
            }
        }
    }

    /** Free native model memory under system pressure; reloaded lazily on next embed. */
    fun release() {
        synchronized(lock) { close() }
    }

    fun embed(text: String, maxLen: Int = 77): FloatArray? {
        synchronized(lock) {
            if (model == null && !loadLocked()) return null
            val inBufs = inputBuffers ?: return null
            val outBufs = outputBuffers ?: return null
            val tok = tokenizer ?: return null
            return try {
                val tokenIds = tok.encode(text, maxLen)
                inBufs[0].writeLong(tokenIds.map { it.toLong() }.toLongArray())
                model!!.run(inBufs, outBufs)
                val emb = outBufs[0].readFloat()
                if (hasInvalidValues(emb)) null else l2Normalize(emb)
            } catch (e: Exception) {
                LogCat.e("TextEmbedHelper: inference failed for: $text", e)
                null
            }
        }
    }

    /** Create the model from the remembered files; caller must hold [lock]. */
    private fun loadLocked(): Boolean {
        val mf = modelFile?.takeIf { it.exists() } ?: return false
        val tf = tokenizerFile?.takeIf { it.exists() } ?: return false
        return try {
            val m = DelegateHelper.createModel(mf)
            model = m
            tokenizer = CLIPTokenizer(tf)
            inputBuffers = m.createInputBuffers()
            outputBuffers = m.createOutputBuffers()
            true
        } catch (e: Throwable) {
            LogCat.e("TextEmbedHelper: model load failed", e)
            false
        }
    }

    /** Release native resources but keep the remembered files for lazy reload. */
    fun close() {
        model?.let { DelegateHelper.close(it) }
        model = null
        tokenizer = null
        inputBuffers = null; outputBuffers = null
    }
}
