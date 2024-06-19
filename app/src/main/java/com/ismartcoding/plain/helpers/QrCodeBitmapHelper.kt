package com.ismartcoding.plain.helpers

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.ismartcoding.lib.logcat.LogCat
import java.io.IOException

object QrCodeBitmapHelper {
    private const val MAX_BITMAP_SIZE = 1024 // Maximum allowed bitmap size (in pixels)

    fun getBitmapFromUri(context: Context, imageUri: Uri): Bitmap {
        val source = ImageDecoder.createSource(
            context.contentResolver,
            imageUri
        )
        return ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
    }

    fun getBitmapFromContentUri(
        context: Context,
        imageUri: Uri,
    ): Bitmap {
        val options =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

        // Load the bitmap size information without actually loading the bitmap into memory
        BitmapFactory.decodeFileDescriptor(context.contentResolver.openFileDescriptor(imageUri, "r")?.fileDescriptor, null, options)

        // Calculate the sample size based on the desired maximum bitmap size
        options.inSampleSize = BitmapHelper.calculateInSampleSize(options, MAX_BITMAP_SIZE, MAX_BITMAP_SIZE)

        // Reset the options to load the bitmap
        options.inJustDecodeBounds = false

        // Load the bitmap with the calculated sample size
        val bitmap =
            BitmapFactory.decodeFileDescriptor(
                context.contentResolver.openFileDescriptor(imageUri, "r")?.fileDescriptor,
                null,
                options,
            )

        val orientation = getExifOrientationTag(context.contentResolver, imageUri)
        var rotationDegrees = 0
        var flipX = false
        var flipY = false
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipX = true
            ExifInterface.ORIENTATION_ROTATE_90 -> rotationDegrees = 90
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                rotationDegrees = 90
                flipX = true
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> rotationDegrees = 180
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipY = true
            ExifInterface.ORIENTATION_ROTATE_270 -> rotationDegrees = -90
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                rotationDegrees = -90
                flipX = true
            }

            ExifInterface.ORIENTATION_UNDEFINED, ExifInterface.ORIENTATION_NORMAL -> {}
            else -> {}
        }
        return rotateBitmap(bitmap, rotationDegrees, flipX, flipY)
    }

    private fun getExifOrientationTag(
        resolver: ContentResolver,
        imageUri: Uri,
    ): Int {
        // We only support parsing EXIF orientation tag from local file on the device.
        // See also:
        // https://android-developers.googleblog.com/2016/12/introducing-the-exifinterface-support-library.html
        if (ContentResolver.SCHEME_CONTENT != imageUri.scheme &&
            ContentResolver.SCHEME_FILE != imageUri.scheme
        ) {
            return 0
        }
        var exif: ExifInterface
        try {
            resolver.openInputStream(imageUri).use { inputStream ->
                if (inputStream == null) {
                    return 0
                }
                exif = ExifInterface(inputStream)
            }
        } catch (e: IOException) {
            LogCat.e("failed to open file to read rotation meta data: $imageUri, $e")
            return 0
        }
        return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }

    private fun rotateBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int,
        flipX: Boolean,
        flipY: Boolean,
    ): Bitmap {
        val matrix = Matrix()

        // Rotate the image back to straight.
        matrix.postRotate(rotationDegrees.toFloat())

        // Mirror the image along the X or Y axis.
        matrix.postScale(if (flipX) -1.0f else 1.0f, if (flipY) -1.0f else 1.0f)
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // Recycle the old bitmap if it has changed.
        if (rotatedBitmap != bitmap) {
            bitmap.recycle()
        }
        return rotatedBitmap
    }
}
