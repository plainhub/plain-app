package com.ismartcoding.lib.markdown.image.svg;

import android.graphics.Picture;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.ismartcoding.lib.markdown.image.MediaDecoder;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

/**
 * @since 4.2.0
 */
public class SvgPictureMediaDecoder extends MediaDecoder {

    public static final String CONTENT_TYPE = "image/svg+xml";

    @NonNull
    public static SvgPictureMediaDecoder create() {
        return new SvgPictureMediaDecoder();
    }

    @NonNull
    @Override
    public Drawable decode(@Nullable String contentType, @NonNull InputStream inputStream) {

        final SVG svg;
        try {
            svg = SVG.getFromInputStream(inputStream);
        } catch (SVGParseException e) {
            throw new IllegalStateException("Exception decoding SVG", e);
        }

        final Picture picture = svg.renderToPicture();
        return new PictureDrawable(picture);
    }

    @NonNull
    @Override
    public Collection<String> supportedTypes() {
        return Collections.singleton(CONTENT_TYPE);
    }
}
