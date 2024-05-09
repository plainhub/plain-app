package com.ismartcoding.lib.markdown.image

import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import androidx.annotation.IntRange
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.AsyncDrawableSpan

class ImageAsyncDrawableSpan(
    val theme: MarkwonTheme,
    drawable: AsyncDrawable,
    val alignment: Int,
    val replacementTextIsLink: Boolean
) : AsyncDrawableSpan(theme, drawable, alignment, replacementTextIsLink) {

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        @IntRange(from = 0) start: Int,
        @IntRange(from = 0) end: Int,
        fm: FontMetricsInt?
    ): Int {

        // if we have no async drawable result - we will just render text
        val size: Int
        if (drawable.hasResult()) {
            val rect = drawable.bounds
            if (fm != null) {
                // FIXME: make sure the image is in the vertical center of the text, and there is not too much space above and below the image
                val height = rect.height()
                val need = height - (fm.descent - fm.ascent)
                if (need > 0) {
                    fm.descent += need / 2
                    fm.ascent -= need / 2
                }
                fm.top = fm.ascent
                fm.bottom = fm.descent
            }
            size = rect.right
        } else {

            // we will apply style here in case if theme modifies textSize or style (affects metrics)
            if (replacementTextIsLink) {
                theme.applyLinkStyle(paint)
            }

            // NB, no specific text handling (no new lines, etc)
            size = (paint.measureText(text, start, end) + .5f).toInt()
        }
        return size
    }
}