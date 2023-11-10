package com.ismartcoding.lib.markdown.image.data;

import android.net.Uri;
import androidx.annotation.NonNull;

import com.ismartcoding.lib.markdown.image.ImageItem;
import com.ismartcoding.lib.markdown.image.SchemeHandler;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Collections;

/**
 * @since 2.0.0
 */
public class DataUriSchemeHandler extends SchemeHandler {

    public static final String SCHEME = "data";

    @NonNull
    public static DataUriSchemeHandler create() {
        return new DataUriSchemeHandler(DataUriParser.create(), DataUriDecoder.create());
    }

    private static final String START = "data:";

    private final DataUriParser uriParser;
    private final DataUriDecoder uriDecoder;

    @SuppressWarnings("WeakerAccess")
    DataUriSchemeHandler(@NonNull DataUriParser uriParser, @NonNull DataUriDecoder uriDecoder) {
        this.uriParser = uriParser;
        this.uriDecoder = uriDecoder;
    }

    @NonNull
    @Override
    public ImageItem handle(@NonNull String raw, @NonNull Uri uri) {

        if (!raw.startsWith(START)) {
            throw new IllegalStateException("Invalid data-uri: " + raw);
        }

        final String part = raw.substring(START.length());

        final DataUri dataUri = uriParser.parse(part);
        if (dataUri == null) {
            throw new IllegalStateException("Invalid data-uri: " + raw);
        }

        final byte[] bytes;
        try {
            bytes = uriDecoder.decode(dataUri);
        } catch (Throwable t) {
            throw new IllegalStateException("Cannot decode data-uri: " + raw, t);
        }

        if (bytes == null) {
            throw new IllegalStateException("Decoding data-uri failed: " + raw);
        }

        return ImageItem.withDecodingNeeded(
                dataUri.contentType(),
                new ByteArrayInputStream(bytes));
    }

    @NonNull
    @Override
    public Collection<String> supportedSchemes() {
        return Collections.singleton(SCHEME);
    }
}
