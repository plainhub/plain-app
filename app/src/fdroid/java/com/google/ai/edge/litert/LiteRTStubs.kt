// Compile-only stubs for F-Droid — the real LiteRT is excluded from this flavor.
// These satisfy the compiler; the AI code paths are never reached at runtime on F-Droid.
package com.google.ai.edge.litert

enum class Accelerator { CPU, GPU, NPU }

class TensorBuffer {
    fun writeFloat(data: FloatArray) {}
    fun writeLong(data: LongArray) {}
    fun readFloat(): FloatArray = FloatArray(0)
}

class CompiledModel private constructor() {
    class Options(accelerator: Accelerator)

    companion object {
        fun create(modelPath: String, options: Options): CompiledModel = CompiledModel()
    }

    fun createInputBuffers(): List<TensorBuffer> = emptyList()
    fun createOutputBuffers(): List<TensorBuffer> = emptyList()
    fun run(inputs: List<TensorBuffer>, outputs: List<TensorBuffer>) {}
    fun close() {}
}
