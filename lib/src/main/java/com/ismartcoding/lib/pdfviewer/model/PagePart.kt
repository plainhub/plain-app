package com.ismartcoding.lib.pdfviewer.model

import android.graphics.Bitmap
import android.graphics.RectF

class PagePart(val page: Int, val renderedBitmap: Bitmap?, val pageRelativeBounds: RectF, val isThumbnail: Boolean, var cacheOrder: Int) {
    override fun equals(obj: Any?): Boolean {
        if (obj !is PagePart) {
            return false
        }
        return obj.page == page && obj.pageRelativeBounds.left == pageRelativeBounds.left &&
            obj.pageRelativeBounds.right == pageRelativeBounds.right &&
            obj.pageRelativeBounds.top == pageRelativeBounds.top &&
            obj.pageRelativeBounds.bottom == pageRelativeBounds.bottom
    }
}
