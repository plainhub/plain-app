package com.ismartcoding.plain.ai

import com.ismartcoding.plain.lib.coIO
import kotlinx.coroutines.flow.map

object ImageSearchManager {
    val status = RustImageModels.snapshot.map { it.status }
    val downloadProgress = RustImageModels.snapshot.map { it.downloadProgress }
    val errorMessage = RustImageModels.snapshot.map { it.errorMessage }
    fun getModelDir(): String = RustImageModels.snapshot.value.modelDir
    fun imageModelPath(): String = RustImageModels.snapshot.value.imageModel
    fun isModelReady(): Boolean = RustImageModels.snapshot.value.status == ImageSearchStatusType.READY
    suspend fun restoreIfEnabled() { RustImageModels.call(ImageModelsCommand.Restore) }
    suspend fun enableAsync() { RustImageModels.call(ImageModelsCommand.Enable) }
    suspend fun disableAsync() { RustImageModels.call(ImageModelsCommand.Disable) }
    fun cancelDownload() { coIO { RustImageModels.call(ImageModelsCommand.Cancel) } }
    suspend fun search(query: String, limit: Int = 50): List<SemanticSearchResult> = RustImageModels.search(query,limit)
    fun setIndexError(message: String) { coIO { RustImageModels.call(ImageModelsCommand.Error(message)) } }
}
