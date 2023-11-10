package com.ismartcoding.lib.markdown

import android.text.TextUtils
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.RenderProps
import io.noties.markwon.html.CssInlineStyleParser
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.tag.SimpleTagHandler
import io.noties.markwon.image.ImageProps
import io.noties.markwon.image.ImageSize
import org.commonmark.node.Image

class AppImageHandler internal constructor(private val imageSizeParser: ImageSizeParser) : SimpleTagHandler() {
    override fun supportedTags(): Collection<String> {
        return setOf("img")
    }

    internal interface ImageSizeParser {
        fun parse(attributes: Map<String, String>): ImageSize?
    }

    override fun getSpans(
        configuration: MarkwonConfiguration,
        renderProps: RenderProps,
        tag: HtmlTag
    ): Any? {
        val attributes = tag.attributes()
        val src = attributes["src"]
        if (TextUtils.isEmpty(src)) {
            return null
        }
        val spanFactory = configuration.spansFactory().get(Image::class.java) ?: return null
        val destination = configuration.imageDestinationProcessor().process(src!!)
        val imageSize = imageSizeParser.parse(tag.attributes())

        // todo: replacement text is link... as we are not at block level
        // and cannot inspect the parent of this node... (img and a are both inlines)
        //
        // but we can look and see if we are inside a LinkSpan (will have to extend TagHandler
        // to obtain an instance SpannableBuilder for inspection)
        ImageProps.DESTINATION[renderProps] = destination
        ImageProps.IMAGE_SIZE[renderProps] = imageSize
        ImageProps.REPLACEMENT_TEXT_IS_LINK[renderProps] = false
        // remove margin from image





        return spanFactory.getSpans(configuration, renderProps)
    }

    companion object {
        fun create(): AppImageHandler {
            return AppImageHandler(AppImageSizeParserImpl(CssInlineStyleParser.create()))
        }
    }
}