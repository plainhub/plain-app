package com.ismartcoding.plain.helpers

import androidx.exifinterface.media.ExifInterface
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.enums.ImageType
import java.io.File
import java.io.IOException
import kotlin.experimental.and

object ImageHelper {
    /**
     * https://www.garykessler.net/library/file_sigs.html
     */

    // https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
    private const val GIF_HEADER_87A = "GIF87a"
    private const val GIF_HEADER_89A = "GIF89a"

    // https://developers.google.com/speed/webp/docs/riff_container
    private const val WEBP_HEADER_RIFF = "RIFF"
    private const val WEBP_HEADER_WEBP = "WEBP"
    private const val WEBP_HEADER_VPX8 = "VP8X"

    // https://nokiatech.github.io/heif/technical.html
    private const val HEIF_HEADER_FTYP = "ftyp"
    private const val HEIF_HEADER_MSF1 = "msf1"
    private const val HEIF_HEADER_HEVC = "hevc"
    private const val HEIF_HEADER_HEVX = "hevx"

    private const val SVG_TAG = "<svg"
    private const val LEFT_ANGLE_BRACKET = '<'

    fun getImageType(path: String): ImageType {
        val extension = path.getFilenameExtension()
        if (extension == "svg") {
            return ImageType.SVG
        } else if (extension == "png") {
            return ImageType.PNG
        }else if (extension == "jpg" || extension == "jpeg") {
            return ImageType.JPG
        }

        File(path).inputStream().use {
            val totalBytes = it.available()
            val bytes = ByteArray(256)
            it.read(bytes)

            if (bytes[0].toInt() == -1 && bytes[1].toInt() == -40) {
                // jpg
                return ImageType.JPG
            }

            if (bytes[0].toInt() == -119 && bytes[1].toInt() == 80) {
                // png
                return ImageType.PNG
            }

            val info = bytes.decodeToString()
            if (info.substring(0, 4) == WEBP_HEADER_RIFF && info.substring(8, 12) == WEBP_HEADER_WEBP) {
                // webp
                if (info.substring(12, 16) == WEBP_HEADER_VPX8 &&
                    totalBytes > 17 &&
                    (bytes[16] and 0b00000010) > 0
                ) {
                    // 动态 webp
                    return ImageType.WEBP_ANIMATE
                }
                return ImageType.WEBP
            }

            val gifInfo = info.substring(0, 6)
            if (gifInfo == GIF_HEADER_89A || gifInfo == GIF_HEADER_87A) {
                // gif
                return ImageType.GIF
            }

            if (info.substring(4, 8) == HEIF_HEADER_FTYP) {
                // heif
                val heifAnimateInfo = info.substring(8, 12)
                if (heifAnimateInfo == HEIF_HEADER_MSF1 ||
                    heifAnimateInfo == HEIF_HEADER_HEVC ||
                    heifAnimateInfo == HEIF_HEADER_HEVX
                ) {
                    // 动态 heif
                    return ImageType.HEIF_ANIMATED
                }
                return ImageType.HEIF
            }

            if (info.contains(SVG_TAG)) {
                // svg
                return ImageType.SVG
            }
        }

        return ImageType.UNKNOWN
    }

    fun getImageRotation(path: String): Int {
        if (path.endsWith(".svg", true)) {
            return 0
        }
        try {
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: IOException) {
            e.printStackTrace()
            LogCat.e(e.toString())
        }
        return 0
    }
}