package com.ismartcoding.plain.ui.extensions

import android.annotation.SuppressLint
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.markdown.AppImageHandler
import com.ismartcoding.lib.markdown.AppImageSchemeHandler
import com.ismartcoding.lib.markdown.FontTagHandler
import com.ismartcoding.lib.markdown.NetworkSchemeHandler
import com.ismartcoding.lib.markdown.image.ImagesPlugin
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.preview.PreviewDialog
import com.ismartcoding.plain.ui.preview.PreviewItem
import io.noties.markwon.*
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.core.spans.LinkSpan
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImageProps
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import org.commonmark.node.Image
import org.commonmark.node.SoftLineBreak

@SuppressLint("ClickableViewAccessibility")
fun TextView.setSelectableTextClickable(click: () -> Unit) {
    val detector = GestureDetectorCompat(context, GestureDetector.SimpleOnGestureListener())
    detector.setOnDoubleTapListener(
        object : GestureDetector.OnDoubleTapListener {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                click()
                return false
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                return false
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                return false
            }
        },
    )

    setOnTouchListener { _, event ->
        detector.onTouchEvent(event)
        false
    }
}

@SuppressLint("ClickableViewAccessibility")
fun TextView.setDoubleCLick(click: () -> Unit) {
    val detector = GestureDetectorCompat(context, GestureDetector.SimpleOnGestureListener())
    detector.setOnDoubleTapListener(
        object : GestureDetector.OnDoubleTapListener {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return false
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                setTextIsSelectable(false) // deselect text
                click()
                setTextIsSelectable(true)
                return false
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                return false
            }
        },
    )

    setOnTouchListener { _, event ->
        detector.onTouchEvent(event)
        false
    }
}

fun TextView.markdown(content: String) {
    this.movementMethod = LinkMovementMethod.getInstance()
    Markwon.builder(context)
        .usePlugin(GlideImagesPlugin.create(context))
        .usePlugin(
            object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        .bulletWidth(context.dp2px(5))
                }
            },
        )
        .usePlugin(
            ImagesPlugin.create { plugin ->
                plugin.addSchemeHandler(AppImageSchemeHandler(context))
                plugin.addSchemeHandler(NetworkSchemeHandler())
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
                            PreviewDialog().show(
                                items = arrayListOf(PreviewItem(link, Uri.parse(link))),
                                initKey = link,
                            )
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
        .build().setMarkdown(this, content)
}
