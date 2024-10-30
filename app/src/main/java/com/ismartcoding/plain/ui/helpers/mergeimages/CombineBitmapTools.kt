package com.ismartcoding.plain.ui.helpers.mergeimages

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.media.ThumbnailUtils

object CombineBitmapTools {
    fun combineBitmap(
        combineWidth: Int,
        combineHeight: Int,
        bitmaps: List<Bitmap>,
    ): Bitmap {
        val len = bitmaps.size
        val combineBitmapEntities = CombineNineRect.generateCombineBitmapEntity(combineWidth, combineHeight, len)
        val thumbnailBitmaps: MutableList<Bitmap> = ArrayList()
        for (i in 0 until len) {
            thumbnailBitmaps.add(
                ThumbnailUtils.extractThumbnail(
                    bitmaps[i],
                    combineBitmapEntities[i].width.toInt(),
                    combineBitmapEntities[i].height.toInt(),
                ),
            )
        }

        return getCombineBitmaps(
            combineBitmapEntities,
            thumbnailBitmaps,
            combineWidth,
            combineHeight,
        )
    }

    private fun getCombineBitmaps(
        mEntityList: List<CombineBitmapEntity>,
        bitmaps: List<Bitmap>,
        width: Int,
        height: Int,
    ): Bitmap {
        var newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (i in mEntityList.indices) {
            newBitmap =
                mixtureBitmap(
                    newBitmap, bitmaps[i],
                    PointF(
                        mEntityList[i].x, mEntityList[i].y,
                    ),
                )
        }
        return newBitmap
    }

    private fun mixtureBitmap(
        first: Bitmap,
        second: Bitmap,
        fromPoint: PointF,
    ): Bitmap {
        val newBitmap =
            Bitmap.createBitmap(
                first.width,
                first.height,
                Bitmap.Config.ARGB_8888,
            )
        val cv = Canvas(newBitmap)
        cv.drawBitmap(first, 0f, 0f, null)
        cv.drawBitmap(second, fromPoint.x, fromPoint.y, null)
        cv.save()
        cv.restore()
        first.recycle()
        second.recycle()
        return newBitmap
    }
}

class CombineBitmapEntity {
    var x = 0f
    var y = 0f
    var width = 0f
    var height = 0f
    var index = -1

    override fun toString(): String {
        return (
            "MyBitmap [x=" + x + ", y=" + y + ", width=" + width +
                ", height=" + height + ", device=" + devide + ", index=" +
                index + "]"
        )
    }

    companion object {
        var devide = 1
    }
}
