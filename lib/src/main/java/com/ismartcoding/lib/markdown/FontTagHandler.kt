package com.ismartcoding.lib.markdown

import android.graphics.Color
import android.text.style.ForegroundColorSpan
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.RenderProps
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.tag.SimpleTagHandler
import java.util.*

class FontTagHandler() : SimpleTagHandler() {
    override fun getSpans(
        configuration: MarkwonConfiguration,
        renderProps: RenderProps,
        tag: HtmlTag,
    ): Any? {
        return try {
            val color = tag.attributes()["color"]
            if (!color.isNullOrEmpty()) {
                ForegroundColorSpan(Color.parseColor(color))
            } else {
                null
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    override fun supportedTags(): Collection<String> {
        return Collections.singleton("font")
    }
}
