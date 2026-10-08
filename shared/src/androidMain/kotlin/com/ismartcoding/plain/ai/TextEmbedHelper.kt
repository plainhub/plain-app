package com.ismartcoding.plain.ai

import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.TensorBuffer
import com.ismartcoding.plain.lib.logcat.LogCat
import java.io.File

object TextEmbedHelper {
    private var model: CompiledModel? = null
    private var inputBuffers: List<TensorBuffer>? = null
    private var outputBuffers: List<TensorBuffer>? = null
    private var modelFile: File? = null

    // Guards init/release against concurrent embed calls (IO threads vs onTrimMemory).
    private val lock = Any()

    fun init(modelFile: File) {
        synchronized(lock) {
            close()
            this.modelFile = modelFile
            if (!loadLocked()) {
                throw IllegalStateException("TextEmbedHelper: model init failed")
            }
        }
    }

    /** Free native model memory under system pressure; reloaded lazily on next embed. */
    fun release() {
        synchronized(lock) { close() }
    }

    fun embed(tokenIds: IntArray): FloatArray? {
        synchronized(lock) {
            if (model == null && !loadLocked()) return null
            val inBufs = inputBuffers ?: return null
            val outBufs = outputBuffers ?: return null
            return try {
                inBufs[0].writeLong(tokenIds.map { it.toLong() }.toLongArray())
                model!!.run(inBufs, outBufs)
                val emb = outBufs[0].readFloat()
                emb
            } catch (e: Exception) {
                LogCat.e("TextEmbedHelper: inference failed", e)
                null
            }
        }
    }

    /** Create the model from the remembered files; caller must hold [lock]. */
    private fun loadLocked(): Boolean {
        val mf = modelFile?.takeIf { it.exists() } ?: return false
        return try {
            val m = DelegateHelper.createModel(mf)
            model = m
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
        inputBuffers = null; outputBuffers = null
    }
}
