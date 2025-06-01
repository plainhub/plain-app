package com.ismartcoding.plain.helpers

import android.graphics.BitmapFactory
import androidx.compose.ui.unit.IntSize
import androidx.exifinterface.media.ExifInterface
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DImageMeta
import com.ismartcoding.plain.enums.ImageType
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import kotlinx.datetime.Instant
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
        } else if (extension == "jpg" || extension == "jpeg") {
            return ImageType.JPG
        }

        val file = File(path)
        if (!file.exists()) {
            return ImageType.UNKNOWN
        }

        try {
            file.inputStream().use {
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
        } catch (e: Exception) {
            LogCat.e(e.toString())
            return ImageType.UNKNOWN
        }

        return ImageType.UNKNOWN
    }

    fun getRotation(path: String): Int {
        if (path.endsWith(".svg", true)) {
            return 0
        }
        
        val file = File(path)
        if (!file.exists()) {
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
        } catch (e: Exception) {
            LogCat.e(e.toString())
        }
        return 0
    }

    fun getIntrinsicSize(path: String, rotation: Int): IntSize {
        val file = File(path)
        if (!file.exists()) {
            return IntSize.Zero
        }
        
        val size = if (path.endsWith(".svg", true)) {
            SvgHelper.getSize(path)
        } else {
            try {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(path, options)
                IntSize(options.outWidth, options.outHeight)
            } catch (e: Exception) {
                LogCat.e(e.toString())
                IntSize.Zero
            }
        }

        if (rotation == 90 || rotation == 270) {
            return IntSize(size.height, size.width)
        }

        return size
    }

    fun getMeta(path: String): DImageMeta? {
        val file = File(path)
        if (!file.exists()) {
            return null
        }

        try {
            val exif = ExifInterface(path)
            val takenAt = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: return null
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
            val make = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""
            val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: ""
            val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
            val colorSpace = exif.getAttributeInt(ExifInterface.TAG_COLOR_SPACE, -1)
            val apertureValue = exif.getAttributeDouble(ExifInterface.TAG_APERTURE_VALUE, 0.0)
            val exposureTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
            val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) ?: ""
            val isoSpeed = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)
            val flash = exif.getAttributeInt(ExifInterface.TAG_FLASH, 0)
            val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
            val exposureProgram = exif.getAttributeInt(ExifInterface.TAG_EXPOSURE_PROGRAM, 0)
            val meteringMode = exif.getAttributeInt(ExifInterface.TAG_METERING_MODE, 0)
            val whiteBalance = exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, 0)
            val creator = exif.getAttribute(ExifInterface.TAG_ARTIST) ?: ""
            val resolutionX = exif.getAttributeInt(ExifInterface.TAG_X_RESOLUTION, 0)
            val resolutionY = exif.getAttributeInt(ExifInterface.TAG_Y_RESOLUTION, 0)
            val description = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION) ?: ""

            return DImageMeta(
                make,
                model,
                width,
                height,
                rotation,
                colorSpaceToString(colorSpace),
                apertureValue,
                exposureTimeToString(exposureTime),
                focalLength,
                isoSpeed,
                convertExifDateTimeToInstant(takenAt),
                flash,
                fNumber,
                exposureProgram,
                meteringMode,
                whiteBalance,
                creator,
                resolutionX,
                resolutionY,
                description
            )
        } catch (e: IOException) {
            e.printStackTrace()
            LogCat.e(e.toString())
        }

        return null
    }

    private fun exposureTimeToString(exposureInSeconds: Double): String {
        val numerator = (1 / exposureInSeconds).toInt()
        if (numerator == Int.MAX_VALUE) {
            return ""
        }
        return "1/$numerator"
    }

    private fun colorSpaceToString(colorSpace: Int): String {
        return when (colorSpace) {
            ExifInterface.COLOR_SPACE_S_RGB -> {
                "sRGB"
            }

            else -> {
                "RGB"
            }
        }
    }

    private fun convertExifDateTimeToInstant(dateTime: String?): Instant? {
        if (dateTime == null) {
            return null
        }

        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
            val localDateTime = LocalDateTime.parse(dateTime, formatter)
            val javaInstant = ZonedDateTime.of(localDateTime, ZoneOffset.UTC).toInstant()

            return Instant.fromEpochMilliseconds(javaInstant.toEpochMilli())
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
        }

        return null
    }

    fun getExposureProgramText(exposureProgram: Int): String {
        return when (exposureProgram.toShort()) {
            ExifInterface.EXPOSURE_PROGRAM_MANUAL -> getString(R.string.exposure_program_manual)
            ExifInterface.EXPOSURE_PROGRAM_NORMAL -> getString(R.string.exposure_program_normal)
            ExifInterface.EXPOSURE_PROGRAM_APERTURE_PRIORITY -> getString(R.string.exposure_program_aperture_priority)
            ExifInterface.EXPOSURE_PROGRAM_SHUTTER_PRIORITY -> getString(R.string.exposure_program_shutter_priority)
            ExifInterface.EXPOSURE_PROGRAM_CREATIVE -> getString(R.string.exposure_program_creative)
            ExifInterface.EXPOSURE_PROGRAM_ACTION -> getString(R.string.exposure_program_action)
            ExifInterface.EXPOSURE_PROGRAM_PORTRAIT_MODE -> getString(R.string.exposure_program_portrait)
            ExifInterface.EXPOSURE_PROGRAM_LANDSCAPE_MODE -> getString(R.string.exposure_program_landscape)
            else -> ""
        }
    }

    fun getMeteringModeText(meteringMode: Int): String {
        return when (meteringMode.toShort()) {
            ExifInterface.METERING_MODE_AVERAGE -> getString(R.string.metering_mode_average)
            ExifInterface.METERING_MODE_CENTER_WEIGHT_AVERAGE -> getString(R.string.metering_mode_center_weight_average)
            ExifInterface.METERING_MODE_MULTI_SPOT -> getString(R.string.metering_mode_multi_spot)
            ExifInterface.METERING_MODE_OTHER -> getString(R.string.metering_mode_other)
            ExifInterface.METERING_MODE_PARTIAL -> getString(R.string.metering_mode_partial)
            ExifInterface.METERING_MODE_PATTERN -> getString(R.string.metering_mode_pattern)
            ExifInterface.METERING_MODE_SPOT -> getString(R.string.metering_mode_spot)
            else -> ""
        }
    }

    fun getWhiteBalanceText(whiteBalance: Int): String {
        return when (whiteBalance.toShort()) {
            ExifInterface.WHITE_BALANCE_AUTO -> getString(R.string.white_balance_auto)
            ExifInterface.WHITE_BALANCE_MANUAL -> getString(R.string.white_balance_manual)
            else -> ""
        }
    }

    fun getFlashText(flash: Int): String {
        return when (flash.toShort()) {
            ExifInterface.FLAG_FLASH_FIRED -> getString(R.string.flash_fired)
            ExifInterface.FLAG_FLASH_RETURN_LIGHT_NOT_DETECTED -> getString(R.string.flash_return_light_not_detected)
            ExifInterface.FLAG_FLASH_RETURN_LIGHT_DETECTED -> getString(R.string.flash_return_light_detected)
            ExifInterface.FLAG_FLASH_MODE_COMPULSORY_FIRING -> getString(R.string.flash_mode_compulsory_firing)
            ExifInterface.FLAG_FLASH_MODE_COMPULSORY_SUPPRESSION -> getString(R.string.flash_mode_compulsory_suppression)
            ExifInterface.FLAG_FLASH_MODE_AUTO -> getString(R.string.flash_mode_auto)
            ExifInterface.FLAG_FLASH_NO_FLASH_FUNCTION -> getString(R.string.flash_no_flash_function)
            ExifInterface.FLAG_FLASH_RED_EYE_SUPPORTED -> getString(R.string.flash_red_eye_supported)
            else -> ""
        }
    }
}

