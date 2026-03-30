package com.ismartcoding.plain.ai

import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.TensorBuffer
import com.ismartcoding.lib.logcat.LogCat
import java.io.File

object TextEmbedHelper {
    private var model: CompiledModel? = null
    private var tokenizer: CLIPTokenizer? = null
    private var inputBuffers: List<TensorBuffer>? = null
    private var outputBuffers: List<TensorBuffer>? = null

    fun init(modelFile: File, tokenizerFile: File) {
        close()
        val m = DelegateHelper.createModel(modelFile)
        model = m
        tokenizer = CLIPTokenizer(tokenizerFile)
        inputBuffers = m.createInputBuffers()
        outputBuffers = m.createOutputBuffers()
    }

    fun embed(text: String, maxLen: Int = 77): FloatArray? {
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

    fun close() {
        model?.close(); model = null
        tokenizer = null
        inputBuffers = null; outputBuffers = null
    }
}
