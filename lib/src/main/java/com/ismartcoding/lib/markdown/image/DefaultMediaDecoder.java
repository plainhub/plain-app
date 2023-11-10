package com.ismartcoding.lib.markdown.image;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

/**
 * This class can be used as the last {@link MediaDecoder} to _try_ to handle all rest cases.
 * Here we just assume that supplied InputStream is of image type and try to decode it.
 *
 * <strong>NB</strong> if you are dealing with big images that require down scaling see {@link DefaultDownScalingMediaDecoder}
 * which additionally down scales displayed images.
 *
 * @see DefaultDownScalingMediaDecoder
 * @since 1.1.0
 */
public class DefaultMediaDecoder extends MediaDecoder {

    @NonNull
    public static DefaultMediaDecoder create() {
        return new DefaultMediaDecoder(Resources.getSystem());
    }

    @NonNull
    public static DefaultMediaDecoder create(@NonNull Resources resources) {
        return new DefaultMediaDecoder(resources);
    }

    private final Resources resources;

    @SuppressWarnings("WeakerAccess")
    DefaultMediaDecoder(Resources resources) {
        this.resources = resources;
    }

    @NonNull
    @Override
    public Drawable decode(@Nullable String contentType, @NonNull InputStream inputStream) {

        final Bitmap bitmap;
        try {
            // absolutely not optimal... thing
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (Throwable t) {
            throw new IllegalStateException("Exception decoding input-stream", t);
        }

        return new BitmapDrawable(resources, bitmap);
    }

    @NonNull
    @Override
    public Collection<String> supportedTypes() {
        return Collections.emptySet();
    }
}
