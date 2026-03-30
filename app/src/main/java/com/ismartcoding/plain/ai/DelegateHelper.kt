package com.ismartcoding.plain.ai

import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.ismartcoding.lib.logcat.LogCat
import java.io.File

object DelegateHelper {
    private val models = mutableListOf<CompiledModel>()

    fun createModel(modelFile: File): CompiledModel {
        tryGpu(modelFile)?.let { return it }
        LogCat.d("Using CPU for ${modelFile.name}")
        val model = CompiledModel.create(
            modelFile.absolutePath,
            CompiledModel.Options(Accelerator.CPU),
        )
        models.add(model)
        return model
    }

    fun closeAll() {
        models.forEach { it.close() }
        models.clear()
    }

    private fun tryGpu(modelFile: File): CompiledModel? {
        return try {
            val model = CompiledModel.create(
                modelFile.absolutePath,
                CompiledModel.Options(Accelerator.GPU),
            )
            models.add(model)
            LogCat.d("GPU accelerator enabled for ${modelFile.name}")
            model
        } catch (e: Exception) {
            LogCat.d("GPU accelerator unavailable: ${e.message}")
            null
        }
    }
}
