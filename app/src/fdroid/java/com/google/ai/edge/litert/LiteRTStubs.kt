// Compile-only stubs for F-Droid — the real LiteRT is excluded from this flavor.
// These satisfy the compiler; the AI code paths are never reached at runtime on F-Droid.
package com.google.ai.edge.litert

enum class Accelerator { NONE, CPU, GPU, NPU }

class TensorBuffer {
    fun writeFloat(data: FloatArray) {}
    fun writeLong(data: LongArray) {}
    fun readFloat(): FloatArray = FloatArray(0)
}

class CompiledModel private constructor() {
    // Must use vararg to match the real LiteRT API signature
    // `<init>([Lcom/google/ai/edge/litert/Accelerator;)V`. The shared module is
    // compiled against the real LiteRT (compileOnly), so its bytecode calls the
    // varargs constructor. A single-arg stub would cause NoSuchMethodError at
    // runtime on F-Droid.
    class Options(vararg accelerators: Accelerator)

    companion object {
        fun create(modelPath: String, options: Options): CompiledModel =
            throw RuntimeException("LiteRT is not available in F-Droid build")
    }

    fun createInputBuffers(): List<TensorBuffer> = emptyList()
    fun createOutputBuffers(): List<TensorBuffer> = emptyList()
    fun run(inputs: List<TensorBuffer>, outputs: List<TensorBuffer>) {}
    fun close() {}
}
