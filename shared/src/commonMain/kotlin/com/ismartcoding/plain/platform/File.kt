package com.ismartcoding.plain.platform

import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.web.http.StreamSink

expect fun getFileId(path: String): String

suspend fun releaseAppFile(fidSuffix: String) = withIO {
    val hash = fidSuffix.substringBefore(".")
    val dao = AppDatabase.instance.appFileDao()
    dao.decrementRefCount(hash)
    val updated = dao.getById(hash) ?: return@withIO
    if (updated.refCount <= 0) {
        dao.delete(hash)
        deleteFileAt(updated.realPath)
        LogCat.d("AppFileStore: deleted orphan file $hash")
    }
}

expect fun deleteFileAt(path: String)

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

/**
 * List uploaded chunk files for [fileId]. Each entry is "<index>:<size>".
 * Returns an empty list if no chunks have been uploaded.
 */
expect fun listUploadedChunks(fileId: String): List<String>

/**
 * Delete all uploaded chunk files for [fileId]. Returns true on success.
 */
expect fun deleteUploadedChunks(fileId: String): Boolean

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
expect suspend fun mergeUploadedChunks(
    fileId: String,
    totalChunks: Int,
    path: String,
    replace: Boolean,
    isAppFile: Boolean,
): String

/**
 * Save an uploaded chunk ([data]) for [fileId] at [chunkIndex] into the upload
 * tmp directory. Returns the absolute path of the saved chunk file.
 */
expect fun saveUploadChunk(fileId: String, chunkIndex: Int, data: ByteArray): String

/**
 * Stream the contents of the file at [path] into [sink]. Returns true on success,
 * false if the file cannot be opened. The [sink] is NOT closed by this call.
 */
expect suspend fun streamFileTo(path: String, sink: StreamSink): Boolean

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
 * Returns the MIME type of a file path based on a platform content resolver or
 * extension lookup. May return null when the type cannot be determined.
 */
expect fun getContentTypeForPath(path: String): String?

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
 * Look up a media-scanned file (e.g. from MediaStore.Files) by its [mediaId]
 * and return its [DFile] representation, or null if not found.
 *
 * On iOS, always returns null (no MediaStore equivalent).
 */
expect suspend fun getFileByMediaId(mediaId: String): DFile?
