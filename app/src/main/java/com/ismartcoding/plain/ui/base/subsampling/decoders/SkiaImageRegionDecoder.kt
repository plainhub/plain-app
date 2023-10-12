package com.ismartcoding.plain.ui.base.subsampling.decoders

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory.Options
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.plain.ui.base.subsampling.ComposeSubsamplingScaleImageDecoder
import com.ismartcoding.plain.ui.base.subsampling.helpers.Try
import java.io.InputStream
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class SkiaImageRegionDecoder(
  private val bitmapConfig: Bitmap.Config
) : ComposeSubsamplingScaleImageDecoder {
  @Volatile private var decoder: BitmapRegionDecoder? = null
  private val decoderLock = ReentrantReadWriteLock(true)

  override fun init(context: Context, inputStream: InputStream): Result<IntSize> {
    return Result.Try {
      decoder = BitmapRegionDecoder.newInstance(inputStream, false)
      return@Try IntSize(decoder!!.width, decoder!!.height)
    }
  }

  override fun decodeRegion(sRect: Rect, sampleSize: Int): Result<Bitmap> {
    return Result.Try {
      return@Try decoderLock.read {
        if (decoder != null && !decoder!!.isRecycled) {
          val options = Options()
          options.inSampleSize = sampleSize
          options.inPreferredConfig = bitmapConfig

          return@read decoder!!.decodeRegion(sRect, options)
            ?: throw Exception("Failed to initialize image decoder: ${javaClass.simpleName}. Image format may not be supported")
        } else {
          throw IllegalStateException("Cannot decode region after decoder has been recycled")
        }
      }
    }
  }

  override fun isReady(): Boolean {
    return decoder != null && !decoder!!.isRecycled
  }

  override fun recycle() {
    decoderLock.write {
      decoder?.recycle()
      decoder = null
    }
  }

}