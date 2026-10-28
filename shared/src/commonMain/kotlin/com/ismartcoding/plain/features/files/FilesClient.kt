package com.ismartcoding.plain.features.files

import com.ismartcoding.plain.features.files.upload.RustUploadChunks
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.File as FileEntry
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import io.ktor.http.Url
import io.ktor.http.decodeURLPart
import kotlinx.coroutines.CancellationException

object FilesClient {
    suspend fun size(path: String): Long =
        GraphqlApi.execute(Query.fileInfo(path)).size

    suspend fun writeChunks(path: String, chunks: Iterator<ByteArray>): Boolean =
        RustUploadChunks.replace(path, chunks)

    suspend fun copyToPath(srcPath: String, targetPath: String, overwrite: Boolean): Boolean =
        GraphqlApi.execute(Mutation.copyFile(srcPath, targetPath, overwrite))

    suspend fun copyToDirectory(srcPath: String, dirPath: String, fileName: String): String =
        GraphqlApi.execute(Mutation.copyFileToDirectory(srcPath, dirPath, fileName)).orEmpty()

    suspend fun createZipArchive(paths: List<String>, target: String): Boolean = archiveOperation {
        GraphqlApi.execute(Mutation.createZipArchive(paths, target))
    }

    suspend fun extractZipArchive(path: String, target: String): Boolean = archiveOperation {
        GraphqlApi.execute(Mutation.extractZipArchive(path, target))
    }

    suspend fun cacheZipEntry(path: String): String? = try {
        GraphqlApi.execute(Mutation.cacheZipEntry(path))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    suspend fun createDirectory(path: String): FileEntry = write(FileWriteAction.CREATE_DIRECTORY, path)

    suspend fun createFile(path: String): FileEntry = write(FileWriteAction.CREATE_FILE, path)

    suspend fun writeText(
        path: String,
        content: String,
        overwrite: Boolean,
    ): FileEntry = write(FileWriteAction.WRITE_TEXT, path, content, overwrite)

    suspend fun list(root: String, showHidden: Boolean, sortBy: FileSortBy): List<FileEntry> =
        read(root, "show_hidden:$showHidden", sortBy)

    suspend fun searchName(text: String, root: String, showHidden: Boolean, sortBy: FileSortBy): List<FileEntry> =
        read(root, "${literalNameQuery(text)} show_hidden:$showHidden", sortBy)

    suspend fun search(
        query: String,
        root: String,
        sortBy: FileSortBy,
        offset: Int = 0,
        limit: Int? = null,
    ): List<FileEntry> =
        read(root, query, sortBy, offset, limit)

    suspend fun indexed(query: String, limit: Int, offset: Int, sortBy: FileSortBy): List<FileEntry> =
        GraphqlApi.execute(Query.indexedFiles(query, limit, offset, sortBy))

    suspend fun recent(): List<FileEntry> =
        GraphqlApi.execute(Query.recentFiles())

    suspend fun indexedCount(query: String): Int =
        GraphqlApi.execute(Query.indexedFileCount(query))

    private suspend fun archiveOperation(operation: suspend () -> String): Boolean = try {
        operation()
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }

    suspend fun rename(path: String, name: String): String? =
        GraphqlApi.execute(Mutation.renameFile(path, name))

    suspend fun delete(path: String): Boolean = deleteMany(listOf(path)) > 0

    suspend fun deleteMany(paths: List<String>): Int =
        GraphqlApi.execute(Mutation.deleteFiles(paths)).affectedCount

    private suspend fun read(
        root: String,
        query: String,
        sortBy: FileSortBy,
        offset: Int = 0,
        limit: Int? = null,
    ): List<FileEntry> =
        GraphqlApi.execute(Query.files(root, query, sortBy, offset, limit ?: Int.MAX_VALUE))

    internal fun literalNameQuery(text: String): String = buildString {
        append("text:=")
        for (character in text) {
            if (character == '\\' || character == '\'' || character == '"' || character.isWhitespace()) append('\\')
            append(character)
        }
    }

    private suspend fun write(
        action: FileWriteAction,
        path: String,
        content: String = "",
        overwrite: Boolean = false,
    ): FileEntry {
        val target = if (path.startsWith("file://")) Url(path).encodedPath.decodeURLPart() else path
        val operation = Mutation.fileWrite(action, target, content, overwrite)
        return GraphqlApi.execute(operation)
    }
}
