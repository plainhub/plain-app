package com.ismartcoding.lib.markdown.image;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import io.noties.markwon.image.AsyncDrawable;
import io.noties.markwon.image.AsyncDrawableLoader;
import io.noties.markwon.image.DrawableUtils;

class AsyncDrawableLoaderImpl extends AsyncDrawableLoader {

    private final ExecutorService executorService;
    private final Map<String, SchemeHandler> schemeHandlers;
    private final Map<String, MediaDecoder> mediaDecoders;
    private final MediaDecoder defaultMediaDecoder;
    private final ImagesPlugin.PlaceholderProvider placeholderProvider;
    private final ImagesPlugin.ErrorHandler errorHandler;

    private final Handler handler;

    // @since 4.0.0 use a hash-map with a AsyncDrawable as key for multiple requests
    //  for the same destination
    private final Map<AsyncDrawable, Future<?>> requests = new HashMap<>(2);

    AsyncDrawableLoaderImpl(@NonNull AsyncDrawableLoaderBuilder builder) {
        this(builder, new Handler(Looper.getMainLooper()));
    }

    // @since 4.0.0
    @VisibleForTesting
    AsyncDrawableLoaderImpl(@NonNull AsyncDrawableLoaderBuilder builder, @NonNull Handler handler) {
        this.executorService = builder.executorService;
        this.schemeHandlers = builder.schemeHandlers;
        this.mediaDecoders = builder.mediaDecoders;
        this.defaultMediaDecoder = builder.defaultMediaDecoder;
        this.placeholderProvider = builder.placeholderProvider;
        this.errorHandler = builder.errorHandler;
        this.handler = handler;
    }

    @Override
    public void load(@NonNull final AsyncDrawable drawable) {
        final Future<?> future = requests.get(drawable);
        if (future == null) {
            requests.put(drawable, execute(drawable));
        }
    }

    @Override
    public void cancel(@NonNull final AsyncDrawable drawable) {

        final Future<?> future = requests.remove(drawable);
        if (future != null) {
            future.cancel(true);
        }

        handler.removeCallbacksAndMessages(drawable);
    }

    @Nullable
    @Override
    public Drawable placeholder(@NonNull AsyncDrawable drawable) {
        return placeholderProvider != null
                ? placeholderProvider.providePlaceholder(drawable)
                : null;
    }

    @NonNull
    private Future<?> execute(@NonNull final AsyncDrawable asyncDrawable) {
        return executorService.submit(new Runnable() {
            @Override
            public void run() {

                final String destination = asyncDrawable.getDestination();

                final Uri uri = Uri.parse(destination);

                Drawable drawable = null;

                try {

                    final String scheme = uri.getScheme();
                    if (scheme == null
                            || scheme.length() == 0) {
                        throw new IllegalStateException("No scheme is found: " + destination);
                    }

                    // obtain scheme handler
                    final SchemeHandler schemeHandler = schemeHandlers.get(scheme);
                    if (schemeHandler != null) {

                        // handle scheme
                        final ImageItem imageItem = schemeHandler.handle(destination, uri);

                        // if resulting imageItem needs further decoding -> proceed
                        if (imageItem.hasDecodingNeeded()) {

                            final ImageItem.WithDecodingNeeded withDecodingNeeded = imageItem.getAsWithDecodingNeeded();

                            // @since 4.6.2 close input stream
                            try {
                                MediaDecoder mediaDecoder = mediaDecoders.get(withDecodingNeeded.contentType());

                                if (mediaDecoder == null) {
                                    mediaDecoder = defaultMediaDecoder;
                                }

                                if (mediaDecoder != null) {
                                    drawable = mediaDecoder.decode(withDecodingNeeded.contentType(), withDecodingNeeded.inputStream());
                                } else {
                                    // throw that no media decoder is found
                                    throw new IllegalStateException("No media-decoder is found: " + destination);
                                }
                            } finally {
                                try {
                                    withDecodingNeeded.inputStream().close();
                                } catch (IOException e) {
                                    Log.e("MARKWON-IMAGE", "Error closing inputStream", e);
                                }
                            }
                        } else {
                            drawable = imageItem.getAsWithResult().result();
                        }
                    } else {
                        // throw no scheme handler is available
                        throw new IllegalStateException("No scheme-handler is found: " + destination);
                    }

                } catch (Throwable t) {
                    if (errorHandler != null) {
                        drawable = errorHandler.handleError(destination, t);
                    } else {
                        // else simply log the error
                        Log.e("MARKWON-IMAGE", "Error loading image: " + destination, t);
                    }
                }

                final Drawable out = drawable;

                // @since 4.0.0 apply intrinsic bounds (but only if they are empty)
                if (out != null) {
                    final Rect bounds = out.getBounds();
                    //noinspection ConstantConditions
                    if (bounds == null
                            || bounds.isEmpty()) {
                        DrawableUtils.applyIntrinsicBounds(out);
                    }
                }

                handler.postAtTime(new Runnable() {
                    @Override
                    public void run() {
                        // validate that
                        // * request was not cancelled
                        // * out-result is present
                        // * async-drawable is attached
                        final Future<?> future = requests.remove(asyncDrawable);
                        if (future != null
                                && out != null
                                && asyncDrawable.isAttached()) {
                            asyncDrawable.setResult(out);
                        }
                    }
                }, asyncDrawable, SystemClock.uptimeMillis());
            }
        });
    }
}
