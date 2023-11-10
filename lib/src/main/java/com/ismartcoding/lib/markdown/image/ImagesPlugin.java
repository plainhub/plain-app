package com.ismartcoding.lib.markdown.image;

import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.commonmark.node.Image;

import java.util.concurrent.ExecutorService;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.MarkwonSpansFactory;
import io.noties.markwon.image.AsyncDrawable;
import io.noties.markwon.image.AsyncDrawableScheduler;


@SuppressWarnings({"UnusedReturnValue", "WeakerAccess"})
public class ImagesPlugin extends AbstractMarkwonPlugin {

    /**
     * @since 4.0.0
     */
    public interface ImagesConfigure {
        void configureImages(@NonNull ImagesPlugin plugin);
    }

    /**
     * @since 4.0.0
     */
    public interface PlaceholderProvider {
        @Nullable
        Drawable providePlaceholder(@NonNull AsyncDrawable drawable);
    }

    /**
     * @since 4.0.0
     */
    public interface ErrorHandler {

        /**
         * Can optionally return a Drawable that will be displayed in case of an error
         */
        @Nullable
        Drawable handleError(@NonNull String url, @NonNull Throwable throwable);
    }

    @NonNull
    public static ImagesPlugin create() {
        return new ImagesPlugin();
    }

    @NonNull
    public static ImagesPlugin create(@NonNull ImagesConfigure configure) {
        final ImagesPlugin plugin = new ImagesPlugin();
        configure.configureImages(plugin);
        return plugin;
    }

    private final AsyncDrawableLoaderBuilder builder;

    // @since 4.0.0
    ImagesPlugin() {
        this(new AsyncDrawableLoaderBuilder());
    }

    // @since 4.0.0
    @VisibleForTesting
    ImagesPlugin(@NonNull AsyncDrawableLoaderBuilder builder) {
        this.builder = builder;
    }

    /**
     * Optional (by default new cached thread executor will be used)
     *
     * @since 4.0.0
     */
    @NonNull
    public ImagesPlugin executorService(@NonNull ExecutorService executorService) {
        builder.executorService(executorService);
        return this;
    }

    @NonNull
    public ImagesPlugin addSchemeHandler(@NonNull SchemeHandler schemeHandler) {
        builder.addSchemeHandler(schemeHandler);
        return this;
    }

    @NonNull
    public ImagesPlugin addMediaDecoder(@NonNull MediaDecoder mediaDecoder) {
        builder.addMediaDecoder(mediaDecoder);
        return this;
    }

    /**
     * Please note that if not specified a {@link DefaultMediaDecoder} will be used. So
     * if you need to disable default-image-media-decoder specify here own no-op implementation or null.
     *
     * @see DefaultMediaDecoder
     * @since 4.0.0
     */
    @NonNull
    public ImagesPlugin defaultMediaDecoder(@Nullable MediaDecoder mediaDecoder) {
        builder.defaultMediaDecoder(mediaDecoder);
        return this;
    }

    /**
     * @since 4.0.0
     */
    @NonNull
    public ImagesPlugin removeSchemeHandler(@NonNull String scheme) {
        builder.removeSchemeHandler(scheme);
        return this;
    }

    /**
     * @since 4.0.0
     */
    @NonNull
    public ImagesPlugin removeMediaDecoder(@NonNull String contentType) {
        builder.removeMediaDecoder(contentType);
        return this;
    }

    /**
     * @since 4.0.0
     */
    @NonNull
    public ImagesPlugin placeholderProvider(@NonNull PlaceholderProvider placeholderProvider) {
        builder.placeholderProvider(placeholderProvider);
        return this;
    }

    /**
     * @see ErrorHandler
     * @since 4.0.0
     */
    @NonNull
    public ImagesPlugin errorHandler(@NonNull ErrorHandler errorHandler) {
        builder.errorHandler(errorHandler);
        return this;
    }

    @Override
    public void configureConfiguration(@NonNull MarkwonConfiguration.Builder builder) {
        builder.asyncDrawableLoader(this.builder.build());
    }

    @Override
    public void configureSpansFactory(@NonNull MarkwonSpansFactory.Builder builder) {
        builder.setFactory(Image.class, new ImageSpanFactory());
    }

    @Override
    public void beforeSetText(@NonNull TextView textView, @NonNull Spanned markdown) {
        AsyncDrawableScheduler.unschedule(textView);
    }

    @Override
    public void afterSetText(@NonNull TextView textView) {
        AsyncDrawableScheduler.schedule(textView);
    }
}
