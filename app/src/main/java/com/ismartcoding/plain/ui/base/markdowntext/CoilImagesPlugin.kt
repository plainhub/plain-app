package com.ismartcoding.plain.ui.base.markdowntext

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.widget.TextView
import coil3.ImageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.target.Target
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.AsyncDrawableLoader
import io.noties.markwon.image.AsyncDrawableScheduler
import io.noties.markwon.image.DrawableUtils
import io.noties.markwon.image.ImageSpanFactory
import org.commonmark.node.Image
import java.util.concurrent.atomic.AtomicBoolean

class CoilImagesPlugin internal constructor(coilStore: CoilStore, imageLoader: ImageLoader) : AbstractMarkwonPlugin() {
    interface CoilStore {
        fun load(drawable: AsyncDrawable): ImageRequest
        fun cancel(disposable: Disposable)
    }

    private val coilAsyncDrawableLoader = CoilAsyncDrawableLoader(coilStore, imageLoader)

    override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
        builder.setFactory(Image::class.java, ImageSpanFactory())
    }

    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
        builder.asyncDrawableLoader(coilAsyncDrawableLoader)
    }

    override fun beforeSetText(textView: TextView, markdown: Spanned) {
        AsyncDrawableScheduler.unschedule(textView)
    }

    override fun afterSetText(textView: TextView) {
        AsyncDrawableScheduler.schedule(textView)
    }

    private class CoilAsyncDrawableLoader(private val coilStore: CoilStore, private val imageLoader: ImageLoader) : AsyncDrawableLoader() {
        private val cache: MutableMap<AsyncDrawable, Disposable> = HashMap(2)

        override fun load(drawable: AsyncDrawable) {
            val loaded = AtomicBoolean(false)
            val target = AsyncDrawableTarget(drawable, loaded)
            val request = coilStore.load(drawable).newBuilder()
                .target(target)
                .build()
            // @since 4.5.1 execute can return result _before_ disposable is created,
            //  thus `execute` would finish before we put disposable in cache (and thus result is
            //  not delivered)
            val disposable = imageLoader.enqueue(request)
            // if flag was not set, then job is running (else - finished before we got here)
            if (!loaded.get()) {
                // mark flag
                loaded.set(true)
                cache[drawable] = disposable
            }
        }

        override fun cancel(drawable: AsyncDrawable) {
            val disposable = cache.remove(drawable)
            if (disposable != null) {
                coilStore.cancel(disposable)
            }
        }

        override fun placeholder(drawable: AsyncDrawable): Drawable? {
            return null
        }

        private inner class AsyncDrawableTarget(val drawable: AsyncDrawable,val  loaded: AtomicBoolean) : Target {
            fun onSuccess(loadedDrawable: Drawable) {
                // @since 4.5.1 check finished flag (result can be delivered _before_ disposable is created)
                if (cache.remove(drawable) != null
                    || !loaded.get()
                ) {
                    // mark
                    loaded.set(true)
                    if (drawable.isAttached) {
                        DrawableUtils.applyIntrinsicBoundsIfEmpty(loadedDrawable)
                        drawable.setResult(loadedDrawable)
                    }
                }
            }

            fun onStart(placeholder: Drawable?) {
                if (placeholder != null && drawable.isAttached) {
                    DrawableUtils.applyIntrinsicBoundsIfEmpty(placeholder)
                    drawable.setResult(placeholder)
                }
            }

            fun onError(errorDrawable: Drawable?) {
                if (cache.remove(drawable) != null) {
                    if (errorDrawable != null && drawable.isAttached) {
                        DrawableUtils.applyIntrinsicBoundsIfEmpty(errorDrawable)
                        drawable.setResult(errorDrawable)
                    }
                }
            }
        }
    }

    companion object {
        fun create(
            context: Context,
            imageLoader: ImageLoader
        ): CoilImagesPlugin {
            return CoilImagesPlugin(object : CoilStore {
                override fun load(drawable: AsyncDrawable): ImageRequest {
                    return ImageRequest.Builder(context)
                        .data(drawable.destination)
                        .build()
                }

                override fun cancel(disposable: Disposable) {
                    disposable.dispose()
                }
            }, imageLoader)
        }
    }
}