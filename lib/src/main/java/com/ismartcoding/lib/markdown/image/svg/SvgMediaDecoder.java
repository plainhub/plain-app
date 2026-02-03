package com.ismartcoding.lib.markdown.image.svg;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ismartcoding.lib.androidsvg.SVG;
import com.ismartcoding.lib.androidsvg.SVGParseException;
import com.ismartcoding.lib.markdown.image.MediaDecoder;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

public class SvgMediaDecoder extends MediaDecoder {

    public static final String CONTENT_TYPE = "image/svg+xml";

    /**
     * @see #create(Resources)
     * @since 4.0.0
     */
    @NonNull
    public static SvgMediaDecoder create() {
        return create(Resources.getSystem());
    }

    @NonNull
    public static SvgMediaDecoder create(@NonNull Resources resources) {
        return new SvgMediaDecoder(resources);
    }

    private final Resources resources;

    @SuppressWarnings("WeakerAccess")
    SvgMediaDecoder(Resources resources) {
        this.resources = resources;

        // @since 4.0.0
        validate();
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

        final float w = svg.getDocumentWidth();
        final float h = svg.getDocumentHeight();
        final float density = resources.getDisplayMetrics().density;

        final int width = (int) (w * density + .5F);
        final int height = (int) (h * density + .5F);

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        canvas.scale(density, density);
        svg.renderToCanvas(canvas);

        return new BitmapDrawable(resources, bitmap);
    }

    @NonNull
    @Override
    public Collection<String> supportedTypes() {
        return Collections.singleton(CONTENT_TYPE);
    }

    private static void validate() {
    }
}
