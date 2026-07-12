package com.ismartcoding.plain.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.plain.data.DImageMeta
import com.ismartcoding.plain.data.DVideoMeta

expect fun getImageRotation(path: String): Int

expect fun getImageIntrinsicSize(path: String, rotation: Int): IntSize

/** Returns the intrinsic (width, height) of the video at [path], or [IntSize.Zero] on failure. */
expect fun getVideoIntrinsicSize(path: String): IntSize

expect fun getVideoMeta(path: String): DVideoMeta?

expect fun getImageMeta(path: String): DImageMeta?

/**
 * Try to decode a QR code from the image at [path]. Returns the decoded text, or null if
 * no QR code is found (or decoding is not supported on the platform).
 */
expect fun tryDecodeQrCode(path: String): String?

/**
 * Returns the size in bytes of the file at [path], or 0L if the file does not exist.
 */
expect fun fileLength(path: String): Long

/**
 * Rename a media file from [path] to a name derived from [newName] (same directory).
 * Returns the new absolute path on success, or null on failure.
 * On Android this also triggers a MediaScanner scan so the gallery stays in sync.
 */
expect suspend fun renameMediaFile(path: String, newName: String): String?

/** Returns the media duration in milliseconds for the file at [path], or 0L on failure. */
expect fun getMediaDurationMs(path: String): Long

/** Returns the audio duration in milliseconds for the file at [path], or 0L on failure. */
expect fun getAudioDurationMsFromPath(path: String): Long

expect fun generateQrCode(text: String, width: Int, height: Int): ImageBitmap
