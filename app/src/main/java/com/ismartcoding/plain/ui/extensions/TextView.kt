package com.ismartcoding.plain.ui.extensions

import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.markdown.AppImageHandler
import com.ismartcoding.lib.markdown.AppImageSchemeHandler
import com.ismartcoding.lib.markdown.FontTagHandler
import com.ismartcoding.lib.markdown.NetworkSchemeHandler
import com.ismartcoding.lib.markdown.image.ImagesPlugin
import com.ismartcoding.plain.ui.base.markdowntext.CoilImagesPlugin
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.core.spans.LinkSpan
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImageProps
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import org.commonmark.node.Image
import org.commonmark.node.SoftLineBreak
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.jsoup.Jsoup

fun TextView.markdown(content: String, previewerState: MediaPreviewerState) {
    this.movementMethod = LinkMovementMethod.getInstance()
    val markdown = Markwon.builder(context)
        .usePlugin(CoilImagesPlugin(context))
        .usePlugin(
            ImagesPlugin.create { plugin ->
                plugin.addSchemeHandler(AppImageSchemeHandler(context))
                plugin.addSchemeHandler(NetworkSchemeHandler())
            },
        )
        .usePlugin(
            object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        .bulletWidth(context.dp2px(5))
                }
            },
        )
        .usePlugin(HtmlPlugin.create { plugin ->
            plugin.addHandler(FontTagHandler()).addHandler(AppImageHandler.create())
        })
        .usePlugin(LinkifyPlugin.create(Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS))
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(TaskListPlugin.create(context))
        .usePlugin(MarkwonInlineParserPlugin.create())
        .usePlugin(JLatexMathPlugin.create(this.textSize) { builder -> builder.inlinesEnabled(true) })
        .usePlugin(
            object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.linkResolver { _, link ->
                        WebHelper.open(context, link)
                    }
                }
            },
        )
        .usePlugin(
            object : AbstractMarkwonPlugin() {
                override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                    builder.appendFactory(Image::class.java) { configuration, props ->
                        LinkSpan(
                            configuration.theme(),
                            ImageProps.DESTINATION.require(props),
                        ) { _, link ->
                            coMain {
                                val links = extractImageLinksFromHtml(convertMarkdownToHtml(content)).map { PreviewItem(it, it.getFinalPath(context)) }
                                MediaPreviewData.items = links
                                previewerState.open(index = links.indexOfFirst { it.id == link })
                            }
                        }
                    }
                }
            },
        )
        .usePlugin(
            object : AbstractMarkwonPlugin() {
                override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                    builder.on(SoftLineBreak::class.java) { visitor, _ -> visitor.forceNewLine() }
                }
            },
        )
        .build()

    markdown.setMarkdown(this, content)
}

fun extractImageLinksFromHtml(htmlContent: String): List<String> {
    val imageLinks = mutableListOf<String>()

    // Parse the HTML content using Jsoup
    val doc = Jsoup.parse(htmlContent)

    // Select all <img> tags
    val imgTags = doc.select("img")

    // Extract src attributes from <img> tags
    for (imgTag in imgTags) {
        val imageUrl = imgTag.attr("src")
        imageLinks.add(imageUrl)
    }

    return imageLinks
}

fun convertMarkdownToHtml(markdownText: String): String {
    val parser = Parser.builder().build()
    val renderer = HtmlRenderer.builder().build()
    val document = parser.parse(markdownText)
    return renderer.render(document)
}