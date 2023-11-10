package com.ismartcoding.lib.markdown.image;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;

/**
 * @since 2.0.0
 */
public abstract class ImageItem {

    /**
     * Create an {@link ImageItem} with result, so no further decoding is required.
     *
     * @see #withDecodingNeeded(String, InputStream)
     * @see WithResult
     * @since 4.0.0
     */
    @NonNull
    public static ImageItem withResult(@NonNull Drawable drawable) {
        return new WithResult(drawable);
    }

    /**
     * Create an {@link ImageItem} that requires further decoding of InputStream.
     *
     * @see #withResult(Drawable)
     * @see WithDecodingNeeded
     * @since 4.0.0
     */
    @NonNull
    public static ImageItem withDecodingNeeded(
            @Nullable String contentType,
            @NonNull InputStream inputStream) {
        return new WithDecodingNeeded(contentType, inputStream);
    }


    private ImageItem() {
    }

    /**
     * @since 4.0.0
     */
    public abstract boolean hasResult();

    /**
     * @since 4.0.0
     */
    public abstract boolean hasDecodingNeeded();

    /**
     * @see #hasResult()
     * @since 4.0.0
     */
    @NonNull
    public abstract WithResult getAsWithResult();

    /**
     * @see #hasDecodingNeeded()
     * @since 4.0.0
     */
    @NonNull
    public abstract WithDecodingNeeded getAsWithDecodingNeeded();

    /**
     * @since 4.0.0
     */
    public static class WithResult extends ImageItem {

        private final Drawable result;

        private WithResult(@NonNull Drawable drawable) {
            result = drawable;
        }

        @NonNull
        public Drawable result() {
            return result;
        }

        @Override
        public boolean hasResult() {
            return true;
        }

        @Override
        public boolean hasDecodingNeeded() {
            return false;
        }

        @NonNull
        @Override
        public WithResult getAsWithResult() {
            return this;
        }

        @NonNull
        @Override
        public WithDecodingNeeded getAsWithDecodingNeeded() {
            throw new IllegalStateException();
        }
    }

    /**
     * @since 4.0.0
     */
    public static class WithDecodingNeeded extends ImageItem {

        private final String contentType;
        private final InputStream inputStream;

        private WithDecodingNeeded(
                @Nullable String contentType,
                @NonNull InputStream inputStream) {
            this.contentType = contentType;
            this.inputStream = inputStream;
        }

        @Nullable
        public String contentType() {
            return contentType;
        }

        @NonNull
        public InputStream inputStream() {
            return inputStream;
        }

        @Override
        public boolean hasResult() {
            return false;
        }

        @Override
        public boolean hasDecodingNeeded() {
            return true;
        }

        @NonNull
        @Override
        public WithResult getAsWithResult() {
            throw new IllegalStateException();
        }

        @NonNull
        @Override
        public WithDecodingNeeded getAsWithDecodingNeeded() {
            return this;
        }
    }
}
