package com.ismartcoding.lib.markdown

import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import io.noties.markwon.html.CssInlineStyleParser
import io.noties.markwon.image.ImageSize

class AppImageSizeParserImpl(private val inlineStyleParser: CssInlineStyleParser) : AppImageHandler.ImageSizeParser {
    override fun parse(attributes: Map<String, String>): ImageSize? {

        // strictly speaking percents when specified directly on an attribute
        // are not part of the HTML spec (I couldn't find any reference)
        var width: ImageSize.Dimension? = null
        var height: ImageSize.Dimension? = null

        // okay, let's first check styles
        val style = attributes["style"]
        if (!TextUtils.isEmpty(style)) {
            var key: String
            for (cssProperty in inlineStyleParser.parse(style!!)) {
                key = cssProperty.key()
                if ("width" == key) {
                    width = dimension(cssProperty.value())
                } else if ("height" == key) {
                    height = dimension(cssProperty.value())
                }
                if (width != null
                    && height != null
                ) {
                    break
                }
            }
        }
        if (width != null
            && height != null
        ) {
            return ImageSize(width, height)
        }

        // check tag attributes
        if (width == null) {
            width = dimension(attributes["width"])
        }
        if (height == null) {
            height = dimension(attributes["height"])
        }
        return if (width == null
            && height == null
        ) {
            null
        } else ImageSize(width, height)
    }

    @VisibleForTesting
    fun dimension(value: String?): ImageSize.Dimension? {
        if (TextUtils.isEmpty(value)) {
            return null
        }
        val length = value!!.length
        for (i in length - 1 downTo -1 + 1) {
            if (Character.isDigit(value[i])) {
                return try {
                    val `val` = value.substring(0, i + 1).toFloat()
                    val unit: String? = if (i == length - 1) {
                        // no unit info
                        null
                    } else {
                        value.substring(i + 1, length)
                    }
                    ImageSize.Dimension(`val`, unit)
                } catch (e: NumberFormatException) {
                    // value cannot not be represented as a float
                    null
                }
            }
        }
        return null
    }
}