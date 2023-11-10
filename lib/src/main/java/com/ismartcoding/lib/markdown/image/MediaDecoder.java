package com.ismartcoding.lib.markdown.image;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.util.Collection;

/**
 * @since 3.0.0
 */
public abstract class MediaDecoder {

    /**
     * Changes since 4.0.0:
     * <ul>
     * <li>Returns `non-null` drawable</li>
     * <li>Added `contentType` method parameter</li>
     * </ul>
     */
    @NonNull
    public abstract Drawable decode(
            @Nullable String contentType,
            @NonNull InputStream inputStream
    );

    /**
     * @since 4.0.0
     */
    @NonNull
    public abstract Collection<String> supportedTypes();
}
