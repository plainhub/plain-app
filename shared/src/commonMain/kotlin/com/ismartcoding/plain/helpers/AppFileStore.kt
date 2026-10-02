package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.db.DAppFile
import com.ismartcoding.plain.platform.*
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.models.VAppFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

object AppFileStore {
    private const val FIELDS = "id size mimeType realPath refCount weakHash createdAt updatedAt"

    fun toFidUri(fileId: String, ext: String = ""): String = if (ext.isEmpty()) "fid:$fileId" else "fid:$fileId.$ext"
    fun toFidUri(file: DAppFile): String = "fid:${file.realPath.substringAfterLast('/')}"
    fun extFromMime(mimeType: String): String = if (mimeType.isEmpty()) "" else getExtensionFromMimeType(mimeType).lowercase()
    fun extFromFileName(fileName: String, mimeType: String): String {
        val dot = fileName.lastIndexOf('.')
        return (if (dot > 0) fileName.substring(dot + 1).lowercase() else "").ifEmpty { extFromMime(mimeType) }
    }
    fun relativeDestPath(hash: String, ext: String = ""): String {
        require(hash.length == 64 && hash.all { it in '0'..'9' || it in 'a'..'f' }) { "Invalid file hash" }
        require(ext.none { it == '/' || it == '\\' || it == '.' || it.code == 0 }) { "Invalid file extension" }
        val name = if (ext.isEmpty()) hash else "$hash.$ext"
        return "files/${hash.take(2)}/${hash.substring(2, 4)}/$name"
    }
    fun realPathFromId(fidSuffix: String): String {
        val hash = fidSuffix.substringBefore('.')
        val extension = fidSuffix.substringAfter('.', "")
        return "${RustContentApi.directory}/${relativeDestPath(hash, extension)}"
    }
    fun resolveUri(uri: String): String = if (uri.startsWith("fid:", true)) realPathFromId(uri.substring(4)) else uri

    suspend fun importFile(srcPath: String, fileName: String = "", mimeType: String = "", deleteSrc: Boolean = false): DAppFile = withIO {
        if (srcPath.startsWith("content://")) {
            val relative = ".rust-import/${StringHelper.shortUUID()}"
            ensureDir("${appDir()}/.rust-import")
            val temporary = "${appDir()}/$relative"
            try {
                val name = checkNotNull(copyPickedFileToAppStorage(srcPath, relative)) { "Unable to read selected file" }
                val imported = importPath(temporary, fileName.ifEmpty { name }, mimeType, true)
                if (deleteSrc) deleteFileAt(srcPath)
                return@withIO imported
            } finally { deleteFileAt(temporary) }
        }
        importPath(srcPath.removePrefix("file://"), fileName, mimeType, deleteSrc)
    }

    private suspend fun importPath(path: String, name: String, mime: String, deleteSource: Boolean): DAppFile {
        val file = withContext(NonCancellable) {
            RustContentApi.mutate("importAppFile(source: ${gql(path)}, fileName: ${gql(name)}, mimeType: ${gql(mime)}, deleteSource: $deleteSource) { $FIELDS }", longRunning = true)
                .getValue("importAppFile").file()
        }
        try {
            currentCoroutineContext().ensureActive()
            return file
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { release(file.id) }
            throw cancelled
        }
    }

    suspend fun importBytes(data: ByteArray, mimeType: String = ""): DAppFile = withIO {
        val root = "${appDir()}/.rust-import"
        ensureDir(root)
        val temporary = "$root/${StringHelper.shortUUID()}"
        try {
            check(writeBytesToPath(temporary, data)) { "Unable to stage attachment" }
            importPath(temporary, "", mimeType, true)
        } finally { deleteFileAt(temporary) }
    }

    suspend fun getById(id: String): DAppFile? = RustContentApi.query("appFileRecord(id: ${gql(id.substringBefore('.'))}) { $FIELDS }")
        .getValue("appFileRecord").takeUnless { it is JsonNull }?.file()

    suspend fun records(offset: Int, limit: Int, query: String = ""): List<DAppFile> =
        RustContentApi.query("appFileRecords(offset: $offset, limit: $limit, query: ${gql(query)}) { $FIELDS }")
            .getValue("appFileRecords").jsonArray.map { it.file() }

    suspend fun page(offset: Int, limit: Int, query: String = ""): List<VAppFile> =
        RustContentApi.query("appFileRecords(offset: $offset, limit: $limit, query: ${gql(query)}) { $FIELDS fileName }")
            .getValue("appFileRecords").jsonArray.map { VAppFile(it.file(), it.jsonObject.string("fileName")) }
    suspend fun count(query: String = ""): Int = RustContentApi.query("appFileCount(query: ${gql(query)})").getValue("appFileCount").jsonPrimitive.int
    suspend fun displayName(id: String): String = RustContentApi.query("appFiles(offset: 0, limit: 1, query: ${gql("text:$id")}) { fileName }")
        .getValue("appFiles").jsonArray.firstOrNull()?.jsonObject?.string("fileName").orEmpty()
    suspend fun release(id: String) {
        RustContentApi.mutate("releaseAppFile(id: ${gql(id.substringBefore('.'))})")
    }
    private fun JsonElement.file(): DAppFile = jsonObject.let { row ->
        DAppFile(row.string("id"),row.getValue("size").jsonPrimitive.long,row.string("mimeType"),row.string("realPath"),
            row.getValue("refCount").jsonPrimitive.int,row.string("weakHash"),row.instant("createdAt"),row.instant("updatedAt"))
    }
}
