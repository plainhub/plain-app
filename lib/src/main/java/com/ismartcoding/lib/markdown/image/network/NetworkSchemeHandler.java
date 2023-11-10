package com.ismartcoding.lib.markdown.image.network;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ismartcoding.lib.markdown.image.ImageItem;
import com.ismartcoding.lib.markdown.image.SchemeHandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

/**
 * A simple network scheme handler that is not dependent on any external libraries.
 *
 * @see #create()
 * @since 3.0.0
 */
public class NetworkSchemeHandler extends SchemeHandler {

    public static final String SCHEME_HTTP = "http";
    public static final String SCHEME_HTTPS = "https";

    @NonNull
    public static NetworkSchemeHandler create() {
        return new NetworkSchemeHandler();
    }

    @SuppressWarnings("WeakerAccess")
    NetworkSchemeHandler() {

    }

    @NonNull
    @Override
    public ImageItem handle(@NonNull String raw, @NonNull Uri uri) {

        final ImageItem imageItem;
        try {

            final URL url = new URL(raw);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            final int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                final String contentType = contentType(connection.getHeaderField("Content-Type"));
                final InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                imageItem = ImageItem.withDecodingNeeded(contentType, inputStream);
            } else {
                throw new IOException("Bad response code: " + responseCode + ", url: " + raw);
            }

        } catch (IOException e) {
            throw new IllegalStateException("Exception obtaining network resource: " + raw, e);
        }

        return imageItem;
    }

    @NonNull
    @Override
    public Collection<String> supportedSchemes() {
        return Arrays.asList(SCHEME_HTTP, SCHEME_HTTPS);
    }

    @Nullable
    static String contentType(@Nullable String contentType) {

        if (contentType == null) {
            return null;
        }

        final int index = contentType.indexOf(';');
        if (index > -1) {
            return contentType.substring(0, index);
        }

        return contentType;
    }
}
