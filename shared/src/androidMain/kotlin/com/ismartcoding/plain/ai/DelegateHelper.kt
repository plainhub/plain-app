package com.ismartcoding.plain.ai

import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.ismartcoding.plain.lib.logcat.LogCat
import java.io.File

object DelegateHelper {
    private val models = mutableListOf<CompiledModel>()

    fun createModel(modelFile: File): CompiledModel {
        tryAccelerator(modelFile, Accelerator.NPU, "NPU")?.let { return it }
        tryAccelerator(modelFile, Accelerator.GPU, "GPU")?.let { return it }
        LogCat.d("Using CPU for ${modelFile.name}")
        return try {
            val model = CompiledModel.create(
                modelFile.absolutePath,
                CompiledModel.Options(Accelerator.CPU),
            )
            models.add(model)
            model
        } catch (e: Throwable) {
            throw RuntimeException("LiteRT native library unavailable on this device: ${e.message}", e)
        }
    }

    /** Close one model and drop its reference so a later lazy reload does not
     *  leave a stale entry that closeAll() would close twice. */
    fun close(model: CompiledModel) {
        model.close()
        models.remove(model)
    }

    fun closeAll() {
        models.forEach { it.close() }
        models.clear()
    }

    private fun tryAccelerator(
        modelFile: File, accelerator: Accelerator, name: String,
    ): CompiledModel? {
        return try {
            val model = CompiledModel.create(
                modelFile.absolutePath,
                CompiledModel.Options(accelerator),
            )
            models.add(model)
            LogCat.d("$name accelerator enabled for ${modelFile.name}")
            model
        } catch (e: Throwable) {
            LogCat.d("$name accelerator unavailable: ${e.message}")
            null
        }
    }
}
