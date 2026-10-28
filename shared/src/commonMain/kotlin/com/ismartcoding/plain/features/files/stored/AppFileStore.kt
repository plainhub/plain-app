package com.ismartcoding.plain.features.files.stored

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.models.AppFile
import com.ismartcoding.plain.platform.files.queryPickedFileInfo
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.ui.files.stored.VAppFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object AppFileStore {
    fun toFidUri(fileId: String, ext: String = ""): String = if (ext.isEmpty()) "fid:$fileId" else "fid:$fileId.$ext"

    fun toFidUri(file: AppFile): String = "fid:${file.realPath.substringAfterLast('/')}"

    suspend fun importFile(
        srcPath: String,
        fileName: String = "",
        mimeType: String = "",
        deleteSrc: Boolean = false,
    ): AppFile = withIO {
        val platformResource = srcPath.startsWith("content://") || srcPath.startsWith("ph://")
        val name = if (platformResource && fileName.isEmpty()) queryPickedFileInfo(srcPath)?.displayName.orEmpty() else fileName
        importPath(srcPath, name, mimeType, deleteSrc)
    }

    private suspend fun importPath(path: String, name: String, mime: String, deleteSource: Boolean): AppFile {
        val file = withContext(NonCancellable) {
            GraphqlApi.execute(Mutation.importAppFile(path, name, mime, deleteSource))
        }
        try {
            currentCoroutineContext().ensureActive()
            return file
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { release(file.id) }
            throw cancelled
        }
    }

    suspend fun page(offset: Int, limit: Int, query: String = ""): List<VAppFile> =
        GraphqlApi.execute(Query.appFiles(offset, limit, query))
            .map { VAppFile(it, it.fileName) }

    suspend fun count(query: String = ""): Int =
        GraphqlApi.execute(Query.appFileCount(query))

    suspend fun displayName(id: String): String =
        GraphqlApi.execute(Query.appFileName("text:$id")).firstOrNull()?.fileName.orEmpty()

    suspend fun release(id: String) {
        GraphqlApi.execute(Mutation.releaseAppFile(id.substringBefore('.')))
    }
}
