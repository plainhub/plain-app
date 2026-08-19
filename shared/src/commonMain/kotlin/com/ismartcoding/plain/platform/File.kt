package com.ismartcoding.plain.platform

import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.MessageType
import com.ismartcoding.plain.extensions.resolveAppFileRealPath
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.AppFileStore
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.httpserver.http.StreamSink
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import kotlin.time.Instant

// ── Shared construction / detection helpers ────────────────────────────────
// These hold the platform-independent business logic so each actual only keeps
// the lowest-level file I/O. Android is the reference implementation.

/**
 * Build the [DMessageContent] for a long-text file written at [path].
 * Used by [createLongTextFile].
 */
fun buildLongTextMessage(path: String, fileName: String, text: String, size: Long): DMessageContent {
    val summary = text.substring(0, minOf(text.length, Constants.TEXT_FILE_SUMMARY_LENGTH))
    val messageFile = DMessageFile(uri = path, size = size, summary = summary, fileName = fileName)
    return DMessageContent(MessageType.FILES, DMessageFiles(listOf(messageFile)))
}

/**
 * Build the [DFile] record for a text file at [path] with [size] bytes and the
 * given (epoch-ms) modification time. Used by [writeFileText].
 */
fun buildTextFile(path: String, size: Long, updatedAtMillis: Long): DFile = DFile(
    name = path.substringAfterLast('/'),
    path = path,
    permission = "rw",
    createdAt = null,
    updatedAt = Instant.fromEpochMilliseconds(updatedAtMillis),
    size = size,
    isDir = false,
    children = 0,
    mediaId = "",
)

suspend fun releaseAppFile(fidSuffix: String) = withIO {
    val hash = fidSuffix.substringBefore(".")
    val dao = AppDatabase.instance.appFileDao()
    dao.decrementRefCount(hash)
    val updated = dao.getById(hash) ?: return@withIO
    if (updated.refCount <= 0) {
        dao.delete(hash)
        deleteFileAt(updated.realPath.resolveAppFileRealPath())
        LogCat.d("AppFileStore: deleted orphan file $hash")
    }
}

expect fun deleteFileAt(path: String)

// ── Lowest-level file primitives used by shared stores (e.g. AppFileStore) ─

/** Size in bytes of the file at [path], or 0 if it does not exist. */
expect fun fileSize(path: String): Long

/** Copy [srcPath] to [destPath], overwriting an existing destination. false on failure. */
expect fun copyFile(srcPath: String, destPath: String): Boolean

/** Rename/move [fromPath] to [toPath]. false if it cannot be done atomically. */
expect fun moveFile(fromPath: String, toPath: String): Boolean

/** Full-file SHA-256 (64 hex chars). Reads in chunks. Empty string on failure. */
expect suspend fun sha256File(path: String): String

/** SHA-256 of (first 4 KB ++ last 4 KB) of the file at [path]. Reads only those fragments. */
expect suspend fun sha256FileEdges(path: String, size: Long): String

/**
 * Write [bytes] to a regular file at [path], replacing any existing content.
 * Returns true on success, false on failure. The parent directory must exist.
 */
expect fun writeBytesToPath(path: String, bytes: ByteArray): Boolean

expect fun createLongTextFile(text: String): DMessageContent

/**
 * Copy a file into the system Downloads folder.
 * Returns the destination path on success, empty string on failure.
 */
expect fun saveFileToDownloads(path: String, fileName: String): String

/**
 * Convert a filesystem path to a URI string suitable for viewers (e.g. PDF viewer).
 */
expect fun fileToUriString(path: String): String

/**
 * Returns the asset path for the icon representing the given file extension.
 */
expect fun getFileIconPath(extension: String): String

/**
 * Whether a file exists at the given [path].
 */
expect fun fileExists(path: String): Boolean

/**
 * Copy a picked file (identified by URI string) into app storage under [destRelativePath].
 * Returns the display name of the source file, or null on failure.
 */
expect suspend fun copyPickedFileToAppStorage(uriStr: String, destRelativePath: String): String?

/**
 * Write [content] to a text file at [path]. When [overwrite] is false and the file
 * already exists, throws [com.ismartcoding.plain.lib.kgraphql.GraphQLError].
 * Returns the resulting DFile.
 */
expect fun writeFileText(path: String, content: String, overwrite: Boolean): DFile

/**
 * Returns the directory used to store chunked-upload temp files (one sub-directory
 * per [fileId]). The directory may not yet exist; callers should create it as needed.
 */
expect fun getUploadTmpDirPath(): String

/**
 * Returns the directory used to merge chunked uploads into a temp file before
 * the final move/copy to the destination path. The directory may not yet exist.
 */
expect fun getUploadCacheMergeDirPath(): String

/** Names of the files directly inside [path], empty if the directory does not exist. */
expect fun listFilesInDir(path: String): List<String>

/** Recursively delete the directory at [path]. No-op if it does not exist. */
expect fun deleteDirRecursively(path: String)

/** Absolute directory holding the uploaded chunks of [fileId]. */
private fun chunkDir(fileId: String): String = "${getUploadTmpDirPath()}/${fileId}"

/**
 * List uploaded chunk files for [fileId]. Each entry is "<index>:<size>".
 * Returns an empty list if no chunks have been uploaded.
 */
fun listUploadedChunks(fileId: String): List<String> {
    val dir = chunkDir(fileId)
    return listFilesInDir(dir)
        .filter { it.startsWith("chunk_") }
        .mapNotNull { name ->
            val index = name.removePrefix("chunk_").toIntOrNull()
            if (index != null) "${index}:${fileSize("$dir/$name")}" else null
        }
        .sortedBy { it.substringBefore(':').toInt() }
}

/**
 * Delete all uploaded chunk files for [fileId]. Returns true on success.
 */
fun deleteUploadedChunks(fileId: String): Boolean {
    deleteDirRecursively(chunkDir(fileId))
    return true
}

/**
 * Save an uploaded chunk ([data]) for [fileId] at [chunkIndex] into the upload
 * tmp directory. Returns the absolute path of the saved chunk file.
 */
fun saveUploadChunk(fileId: String, chunkIndex: Int, data: ByteArray): String {
    val dir = chunkDir(fileId)
    ensureDir(dir)
    val chunkPath = "$dir/chunk_$chunkIndex"
    writeBytesToPath(chunkPath, data)
    return chunkPath
}

/**
 * Merge the uploaded chunks for [fileId] (expected [totalChunks] parts) into
 * the file at [path]. When [replace] is false and the destination already exists,
 * a new sibling path is used. When [isAppFile] is true, the merged file is imported
 * into the content-addressable AppFileStore and the returned string is
 * "{fidSuffix}:{mergedSize}"; otherwise the merged file is scanned via the media
 * scanner and the returned string is "{baseFileName}:{mergedSize}".
 *
 * Throws [com.ismartcoding.plain.lib.kgraphql.GraphQLError] on missing chunks or
 * integrity check failure.
 */
suspend fun mergeUploadedChunks(
    fileId: String,
    totalChunks: Int,
    path: String,
    replace: Boolean,
    isAppFile: Boolean,
): String = withIO {
    val dir = chunkDir(fileId)
    if (!fileExists(dir)) throw GraphQLError("No chunks found for $fileId")

    var expectedSize = 0L
    for (i in 0 until totalChunks) {
        val chunkPath = "$dir/chunk_$i"
        if (!fileExists(chunkPath)) throw GraphQLError("Missing chunk $i")
        expectedSize += fileSize(chunkPath)
    }

    val mergeDir = getUploadCacheMergeDirPath()
    ensureDir(mergeDir)
    val tempMergePath = "$mergeDir/.merge_tmp_${fileId}_${TimeHelper.nowMillis()}"

    // Stream the chunks into a temp merge file (no full in-memory buffering).
    val sink = createFileSink(tempMergePath)
    try {
        for (i in 0 until totalChunks) {
            if (!streamFileTo("$dir/chunk_$i", sink)) throw GraphQLError("Failed to read chunk $i")
        }
    } finally {
        sink.close()
    }

    val mergedSize = fileSize(tempMergePath)
    if (mergedSize != expectedSize) {
        deleteFileAt(tempMergePath)
        throw GraphQLError("Merge integrity failed: expected $expectedSize, got $mergedSize")
    }

    if (isAppFile) {
        val dFile = AppFileStore.importFile(tempMergePath, "", deleteSrc = true)
        deleteUploadedChunks(fileId)
        return@withIO "${dFile.realPath.substringAfterLast('/')}:$mergedSize"
    }

    var destPath = path
    if (!replace && fileExists(destPath)) {
        destPath = getNewPath(destPath)
    }
    val parent = destPath.substringBeforeLast('/', "")
    if (parent.isNotEmpty()) ensureDir(parent)
    if (fileExists(destPath)) deleteFileAt(destPath)
    if (!moveFile(tempMergePath, destPath)) {
        copyFile(tempMergePath, destPath)
        deleteFileAt(tempMergePath)
    }
    scanFiles(arrayOf(destPath))
    deleteUploadedChunks(fileId)
    return@withIO "${destPath.substringAfterLast('/')}:$mergedSize"
}

/**
 * Stream the contents of the file at [path] into [sink]. Returns true on success,
 * false if the file cannot be opened. The [sink] is NOT closed by this call.
 */
expect suspend fun streamFileTo(path: String, sink: StreamSink): Boolean

/**
 * Read a byte range `[offset, offset + length)` from the regular file at [path].
 * Returns the bytes read (possibly shorter than [length] when the range extends
 * past EOF), an empty array when [offset] is at or past EOF, or null when the
 * file cannot be opened. Used by the `/fs` route to serve chunked ranges over
 * low-throughput transports (e.g. BLE).
 */
expect suspend fun readFileRange(path: String, offset: Long, length: Int): ByteArray?

/**
 * Create a [StreamSink] backed by a new file at [path] (truncating if it exists).
 * The caller is responsible for calling [StreamSink.close].
 */
expect suspend fun createFileSink(path: String): StreamSink

/**
 * Atomically rename [from] to [to]. On platforms without atomic rename, copies then
 * deletes. Returns true on success.
 */
expect suspend fun renameFileAtomic(from: String, to: String): Boolean

/**
 * Ensure the parent directory of [path] exists (creates it if missing).
 */
expect suspend fun ensureParentDir(path: String)

/**
 * Create a unique temp file path with the given [prefix] in the platform cache dir.
 * Does not create the file — only returns the path.
 */
expect suspend fun createTempFilePath(prefix: String): String

/**
 * Import a file (identified by [tempFilePath]) into the content-addressable AppFileStore.
 * When [deleteSrc] is true the source file is deleted after a successful import.
 * Returns "{hash}.{ext}" suffix used to build `fid:` URIs, or null on failure.
 */
expect suspend fun importAppFile(tempFilePath: String, contentType: String, deleteSrc: Boolean): String?

/**
 * Stream the contents of a content:// URI (Android) or a remote resource (iOS)
 * into [sink]. Returns the resolved MIME type, or null if the stream fails.
 */
expect suspend fun streamContentUri(uri: String, sink: StreamSink): String?

/**
 * Convert a 3gp content URI to MP4 bytes. Returns null on platforms without
 * media transcoding support.
 */
expect suspend fun convert3gpToMp4(uri: String): ByteArray?

/**
 * Returns the package icon PNG bytes for the given [packageName], or null if
 * the package is not installed or the icon cannot be encoded.
 */
expect suspend fun getPackageIconBytes(packageName: String): ByteArray?

/**
 * Decode an image file (e.g. HEIF) at [path] to PNG bytes. Returns null if
 * decoding is not supported or fails.
 */
expect suspend fun decodeImageFileToPng(path: String): ByteArray?

/**
 * Whether the file at [path]/[fileName] is an animated image (GIF, animated
 * WebP, animated HEIF) or an SVG. Used by the `/fs` route to decide whether to
 * skip the HEIF-to-PNG conversion path and serve the file as-is.
 */
expect fun isAnimatedImageOrSvg(path: String, fileName: String): Boolean

/**
 * Generate a thumbnail for the file at [path] of the given [width]/[height].
 * When [centerCrop] is true the thumbnail is cropped to fit the aspect ratio.
 * Returns null when the platform cannot produce a thumbnail.
 */
expect suspend fun getThumbnailBytes(
    path: String,
    width: Int,
    height: Int,
    centerCrop: Boolean,
    mediaId: String,
    fileName: String,
): ByteArray?

/**
 * Zip the given [items] into a streaming output sent to [sink]. Each item
 * is a pair of (sourcePath, entryName). Directories are included recursively.
 * Returns true on success.
 */
expect suspend fun streamZipToSink(items: List<ZipStreamEntry>, sink: StreamSink): Boolean

/**
 * Recursively zip the folder at [folderPath] into [sink]. Returns true on success.
 */
expect suspend fun streamZipFolderToSink(folderPath: String, sink: StreamSink): Boolean

/**
 * Stream a directory inside a zip archive as a new zip to [sink].
 * [zipVirtualPath] is a virtual path like `/path/to/file.zip!zip!/internal/dir/`.
 * Returns true on success.
 */
expect suspend fun streamZipInternalDirToSink(zipVirtualPath: String, sink: StreamSink): Boolean

/**
 * Single entry for [streamZipToSink]. [entryName] is the name used inside the
 * archive (may include subdirectory components). When [entryName] is blank the
 * source file's name is used.
 */
data class ZipStreamEntry(
    val sourcePath: String,
    val entryName: String,
)

/**
 * Fetch [url] over HTTP and stream the response body into [sink]. Returns a pair
 * of (statusCode, contentType) or (0, null) on failure.
 */
expect suspend fun fetchUrlToStream(url: String, sink: StreamSink): Pair<Int, String?>

/**
 * Whether [path] points to an Android content:// URI. Always false on iOS.
 */
expect fun isContentUri(path: String): Boolean

/**
 * Search installed packages, media, or app files matching [query] for the purpose
 * of building a zip download. Returns a list of [ZipStreamEntry] with the source
 * path and a display name. Used by the `/zip/files` route.
 *
 * [type] is a [DataType] name (PACKAGE, VIDEO, AUDIO, IMAGE, APP_FILE, FILE).
 * When [type] is FILE, [tempId] holds a temporary key previously stored via
 * `TempHelper` that resolves to the serialized list of [DownloadFileItem]s.
 */
expect suspend fun searchZipItems(type: String, query: String, tempId: String): List<ZipStreamEntry>

/**
 * Read the contents of a text file at [path] as a UTF-8 string.
 *
 * On Android, supports both regular filesystem paths and `content://` URIs
 * (resolved via the platform `ContentResolver`). On iOS, reads from the
 * filesystem directly. Returns an empty string if the file cannot be opened.
 */
expect suspend fun readTextFile(path: String): String

/**
 * Write [bytes] to the file at [uriStr]. On Android [uriStr] may be a
 * `content://` URI (resolved via `ContentResolver.openOutputStream`) or a
 * filesystem path; on iOS it must be a filesystem path. Returns true on
 * success, false on failure.
 */
expect suspend fun writeBytesToUri(uriStr: String, bytes: ByteArray): Boolean

/**
 * Returns the display name of the file at [uriStr] (e.g. via
 * `OpenableColumns.DISPLAY_NAME` on Android), or null if it cannot be
 * determined. Used to surface a friendly file name after export.
 */
expect fun getFileNameFromUri(uriStr: String): String?

/**
 * Metadata for a picked file (URI-based). [displayName] is the user-visible
 * name, [size] is the byte length, [mimeType] is the content type (may be
 * empty). Returned by [queryPickedFileInfo].
 */
data class PickedFileInfo(val displayName: String, val size: Long, val mimeType: String)

/**
 * Query display name, size, and MIME type for a picked file URI. On Android
 * [uriStr] is typically a `content://` URI resolved via `ContentResolver`;
 * on iOS this returns null (no document picker yet). Returns null when the
 * URI cannot be queried.
 */
expect fun queryPickedFileInfo(uriStr: String): PickedFileInfo?

/**
 * Import a picked file (identified by [uriStr]) into the content-addressable
 * chat file store and return its `fid:` URI, or null on failure. On Android
 * this streams the content URI to a temp file then dedups-imports it; on iOS
 * it returns null (no document picker yet).
 */
expect suspend fun importChatFile(uriStr: String, mimeType: String): String?

/**
 * Look up a media-scanned file (e.g. from MediaStore.Files) by its [mediaId]
 * and return its [DFile] representation, or null if not found.
 *
 * On iOS, always returns null (no MediaStore equivalent).
 */
expect suspend fun getFileByMediaId(mediaId: String): DFile?

/**
 * Ensure the directory at [path] exists, creating it (and any missing parents)
 * if necessary. Basic file system operation — not log-specific.
 */
internal expect fun ensureDir(path: String)

/**
 * Append [line] to the text file at [path] and return the resulting file size
 * in bytes. Creates the file if it does not exist. Basic file system operation.
 */
internal expect fun appendLine(path: String, line: String): Long

/**
 * Delete the file at [path] if it exists. Basic file system operation.
 */
internal expect fun deleteFileIfExists(path: String)

/**
 * Rename a file from [from] to [to]. Basic file system operation.
 */
internal expect fun renameFile(from: String, to: String)
