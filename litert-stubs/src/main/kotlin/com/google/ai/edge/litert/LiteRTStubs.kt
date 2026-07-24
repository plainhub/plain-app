package com.google.ai.edge.litert

enum class Accelerator { NONE, CPU, GPU, NPU }

class TensorBuffer {
    fun writeFloat(data: FloatArray) {}
    fun writeLong(data: LongArray) {}
    fun readFloat(): FloatArray = FloatArray(0)
}

class CompiledModel private constructor() {
    class Options(vararg accelerators: Accelerator)

    companion object {
        fun create(modelPath: String, options: Options): CompiledModel =
            throw RuntimeException("LiteRT stubs — runtime not available")
    }

    fun createInputBuffers(): List<TensorBuffer> = emptyList()
    fun createOutputBuffers(): List<TensorBuffer> = emptyList()
    fun run(inputs: List<TensorBuffer>, outputs: List<TensorBuffer>) {}
    fun close() {}
}
