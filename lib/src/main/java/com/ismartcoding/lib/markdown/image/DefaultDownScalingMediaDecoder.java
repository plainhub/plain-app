package com.ismartcoding.lib.markdown.image;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

/**
 * A {@link MediaDecoder} that additionally process media resource to optionally
 * scale it down to fit specified maximum values. Should be used to ensure that no exception is raised
 * whilst rendering ({@code Canvas: trying to draw too large(Xbytes) bitmap}) or {@code OutOfMemoryException} is thrown.
 *
 * <strong>NB</strong> this media decoder will create a temporary file for each incoming media resource,
 * which can have a performance penalty (IO)
 *
 * @since 4.6.2
 */
public class DefaultDownScalingMediaDecoder extends MediaDecoder {

    /**
     * Values {@code <= 0} are ignored, a dimension is considered to be not restrained any limit in such case
     */
    @NonNull
    public static DefaultDownScalingMediaDecoder create(int maxWidth, int maxHeight) {
        return create(Resources.getSystem(), maxWidth, maxHeight);
    }

    @NonNull
    public static DefaultDownScalingMediaDecoder create(
            @NonNull Resources resources,
            int maxWidth,
            int maxHeight
    ) {
        return new DefaultDownScalingMediaDecoder(resources, maxWidth, maxHeight);
    }

    private final Resources resources;
    private final int maxWidth;
    private final int maxHeight;

    private DefaultDownScalingMediaDecoder(@NonNull Resources resources, int maxWidth, int maxHeight) {
        this.resources = resources;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    // https://android.jlelse.eu/loading-large-bitmaps-efficiently-in-android-66826cd4ad53
    @NonNull
    @Override
    public Drawable decode(@Nullable String contentType, @NonNull InputStream inputStream) {

        final File file = writeToTempFile(inputStream);
        try {

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            // initial result when obtaining bounds is discarded
            decode(file, options);

            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
            options.inJustDecodeBounds = false;

            final Bitmap bitmap = decode(file, options);

            return new BitmapDrawable(resources, bitmap);
        } finally {
            // we no longer need the temporary file
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    @NonNull
    private static File writeToTempFile(@NonNull InputStream inputStream) {
        final File file;
        try {
            file = File.createTempFile("markwon", null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        final OutputStream outputStream;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file, false));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }

        final byte[] buffer = new byte[1024 * 8];
        int length;
        try {
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                // ignored
            }
        }

        return file;
    }

    @Nullable
    private static Bitmap decode(@NonNull File file, @NonNull BitmapFactory.Options options) {
        final InputStream is = readFile(file);
        // not yet, still min SDK is 16
        try {
            return BitmapFactory.decodeStream(is, null, options);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // ignored
            }
        }
    }


    @NonNull
    private static InputStream readFile(@NonNull File file) {
        try {
            return new BufferedInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    // see: https://developer.android.com/topic/performance/graphics/load-bitmap.html#load-bitmap
    private static int calculateInSampleSize(@NonNull BitmapFactory.Options options, int maxWidth, int maxHeight) {
        final int w = options.outWidth;
        final int h = options.outHeight;

        final boolean hasMaxWidth = maxWidth > 0;
        final boolean hasMaxHeight = maxHeight > 0;

        final int inSampleSize;
        if (hasMaxWidth && hasMaxHeight) {
            // minimum of both
            inSampleSize = Math.min(calculateInSampleSize(w, maxWidth), calculateInSampleSize(h, maxHeight));
        } else if (hasMaxWidth) {
            inSampleSize = calculateInSampleSize(w, maxWidth);
        } else if (hasMaxHeight) {
            inSampleSize = calculateInSampleSize(h, maxHeight);
        } else {
            // else no sampling, as we have no dimensions to base our calculations on
            inSampleSize = 1;
        }

        return inSampleSize;
    }

    private static int calculateInSampleSize(int actual, int max) {
        int inSampleSize = 1;
        final int half = actual / 2;
        while ((half / inSampleSize) > max) {
            inSampleSize *= 2;
        }
        return inSampleSize;
    }

    @NonNull
    @Override
    public Collection<String> supportedTypes() {
        return Collections.emptySet();
    }
}
