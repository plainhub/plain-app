package com.ismartcoding.lib.markdown.image.gif;

import android.util.Log;

import androidx.annotation.NonNull;

/**
 * @since 4.0.0
 */
public abstract class GifSupport {

    private static boolean HAS_GIF;

    static {
        boolean result;
        try {
            // @since 4.3.1
            Class.forName("pl.droidsonroids.gif.GifDrawable");
            result = true;
        } catch (Throwable t) {
            // @since 4.1.1 instead of printing full stacktrace of the exception,
            // just print a warning to the console
            Log.w("MarkwonImagesPlugin", missingMessage());
            result = false;
        }
        HAS_GIF = result;
    }

    public static boolean hasGifSupport() {
        return HAS_GIF;
    }

    /**
     * @since 4.1.1
     */
    @NonNull
    static String missingMessage() {
        return "`pl.droidsonroids.gif:android-gif-drawable:*` " +
                "dependency is missing, please add to your project explicitly if you " +
                "wish to use GIF media-decoder";
    }

    private GifSupport() {
    }
}
