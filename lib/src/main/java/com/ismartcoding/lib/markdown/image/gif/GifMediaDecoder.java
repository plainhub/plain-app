package com.ismartcoding.lib.markdown.image.gif;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ismartcoding.lib.markdown.image.MediaDecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import pl.droidsonroids.gif.GifDrawable;

/**
 * @since 1.1.0
 */
@SuppressWarnings("WeakerAccess")
public class GifMediaDecoder extends MediaDecoder {

    public static final String CONTENT_TYPE = "image/gif";

    /**
     * Creates a {@link GifMediaDecoder} with {@code autoPlayGif = true}
     *
     * @since 4.0.0
     */
    @NonNull
    public static GifMediaDecoder create() {
        return create(true);
    }

    @NonNull
    public static GifMediaDecoder create(boolean autoPlayGif) {
        return new GifMediaDecoder(autoPlayGif);
    }

    private final boolean autoPlayGif;

    protected GifMediaDecoder(boolean autoPlayGif) {
        this.autoPlayGif = autoPlayGif;

        // @since 4.0.0
        validate();
    }

    @NonNull
    @Override
    public Drawable decode(@Nullable String contentType, @NonNull InputStream inputStream) {

        final byte[] bytes;
        try {
            bytes = readBytes(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read GIF input-stream", e);
        }

        final GifDrawable drawable;
        try {
            drawable = newGifDrawable(bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Exception creating GifDrawable", e);
        }

        if (!autoPlayGif) {
            drawable.pause();
        }

        return drawable;
    }

    @NonNull
    @Override
    public Collection<String> supportedTypes() {
        return Collections.singleton(CONTENT_TYPE);
    }

    @NonNull
    protected GifDrawable newGifDrawable(@NonNull byte[] bytes) throws IOException {
        return new GifDrawable(bytes);
    }

    @NonNull
    protected static byte[] readBytes(@NonNull InputStream stream) throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final int length = 1024 * 8;
        final byte[] buffer = new byte[length];
        int read;
        while ((read = stream.read(buffer, 0, length)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private static void validate() {
        if (!GifSupport.hasGifSupport()) {
            throw new IllegalStateException(GifSupport.missingMessage());
        }
    }
}
