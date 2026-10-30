package com.ismartcoding.plain.platform.files

import com.ismartcoding.plain.models.File as FileEntry

import com.ismartcoding.plain.platform.files.StreamSink

expect fun deleteFileAt(path: String)

/** Size in bytes of the file at [path], or 0 if it does not exist. */
expect fun fileSize(path: String): Long

/**
 * Write [bytes] to a regular file at [path], replacing any existing content.
 * Returns true on success, false on failure. The parent directory must exist.
 */
expect fun writeBytesToPath(path: String, bytes: ByteArray): Boolean

/**
 * Copy a file into the system Downloads folder.
 * Returns the destination path on success, empty string on failure.
 */
expect suspend fun saveFileToDownloads(path: String, fileName: String): String

/**
 * Convert a filesystem path to a URI string suitable for viewers (e.g. PDF viewer).
 */
expect fun fileToUriString(path: String): String

/**
 * Returns the asset path for the icon representing the given file extension.
 */

/**
 * Whether a file exists at the given [path].
 */
expect fun fileExists(path: String): Boolean

/**
 * Ensure the parent directory of [path] exists (creates it if missing).
 */
expect suspend fun ensureParentDir(path: String)

/** Streams a resource into [sink]; returns null when no input is available and propagates read errors. */
expect suspend fun streamContentUri(uri: String, sink: StreamSink): String?

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

fun isContentUri(path: String): Boolean = path.startsWith("content://")

/**
 * Read the contents of a text file at [path] as a UTF-8 string.
 *
 * On Android, supports both regular filesystem paths and `content://` URIs
 * (resolved via the platform `ContentResolver`). On iOS, reads from the
 * filesystem directly. Returns an empty string if the file cannot be opened.
 */
expect suspend fun readTextFile(path: String): String

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
 * Look up a media-scanned file (e.g. from MediaStore.Files) by its [mediaId]
 * and return its [FileEntry] representation, or null if not found.
 *
 * On iOS, always returns null (no MediaStore equivalent).
 */
expect suspend fun getFileByMediaId(mediaId: String): FileEntry?

internal expect suspend fun provideResourceFile(id: String, uri: String)

expect suspend fun copyFileToUri(path: String, uriStr: String): Boolean

internal expect suspend fun readPickedFileBytes(uri: String): ByteArray
