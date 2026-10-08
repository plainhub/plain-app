package com.ismartcoding.plain.platform

import com.ismartcoding.plain.features.file.RustFileHelper
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.AppFileStore
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.StreamSink
import kotlin.time.Instant

// ── Shared construction / detection helpers ────────────────────────────────
// These hold the platform-independent business logic so each actual only keeps
// the lowest-level file I/O. Android is the reference implementation.

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
    childCount = 0,
    mediaId = "",
)

suspend fun releaseAppFile(fidSuffix: String) {
    AppFileStore.release(fidSuffix)
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


/**
 * Copy a file into the system Downloads folder.
 * Returns the destination path on success, empty string on failure.
 */
expect fun saveFileToDownloads(path: String, fileName: String): String

/**
 * Copy [srcPath] into [dirPath] as [fileName], creating the directory when
 * missing. Returns the destination path on success, empty string on failure.
 */
suspend fun copyFileToDir(srcPath: String, dirPath: String, fileName: String): String {
    val dir = dirPath.trimEnd('/')
    if (dir.isEmpty()) return ""
    val dest = "$dir/$fileName"
    ensureParentDir(dest)
    return if (copyFile(srcPath, dest)) dest else ""
}

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
 * already exists, the Rust request fails.
 * Returns the resulting DFile.
 */
suspend fun writeFileText(path: String, content: String, overwrite: Boolean): DFile = RustFileHelper.writeText(path, content, overwrite)

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
 * [fileName] is the original upload file name — its extension is the primary source
 * for the on-disk extension. When [deleteSrc] is true the source file is deleted
 * after a successful import. Returns "{hash}.{ext}" suffix used to build `fid:` URIs,
 * or null on failure.
 */
expect suspend fun importAppFile(tempFilePath: String, fileName: String, contentType: String, deleteSrc: Boolean): String?

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
 * Returns a browser-playable variant of [path] for MP4 files that embed
 * metadata tracks Chromium cannot demux (e.g. the Pixel camera "mebx" motion
 * track): an audio+video-only stream-copy remux, cached on the platform side.
 * Returns null when no conversion is needed or the platform has no remux
 * support.
 */
expect suspend fun remuxMp4ForBrowser(path: String): String?

/**
 * fourcc of the first video track's stsd sample entry (e.g. "hvc1", "avc1"),
 * or "" when the file has no parsable video track. Cheap box-level probe used
 * by the `/fs` `probe=1` mode so the browser can decide whether it can decode
 * the file before committing to a playback URL.
 */
expect suspend fun probeVideoCodec(path: String): String

/**
 * Returns an H.264-transcoded variant of the HEVC video at [path] for
 * browsers without an HEVC decoder (audio stream-copied, cached on the
 * platform side). Returns null when the file is not HEVC, is too long, or
 * the pipeline fails. Only run when the client explicitly opted in (`tr=1`)
 * — transcoding is far more expensive than a remux.
 */
expect suspend fun transcodeMp4ForBrowser(path: String): String?

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
expect suspend fun decodeThumbnailBytes(
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
expect suspend fun streamZipToSink(items: List<ZipStreamEntry>, sink: StreamSink, recursive: Boolean = true): Boolean

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
