package com.ismartcoding.lib.markdown.image.network;

import android.net.Uri;
import androidx.annotation.NonNull;

import com.ismartcoding.lib.markdown.image.ImageItem;
import com.ismartcoding.lib.markdown.image.SchemeHandler;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * @since 4.0.0
 */
public class OkHttpNetworkSchemeHandler extends SchemeHandler {

    /**
     * @see #create(OkHttpClient)
     */
    @NonNull
    public static OkHttpNetworkSchemeHandler create() {
        return create(new OkHttpClient());
    }

    @NonNull
    public static OkHttpNetworkSchemeHandler create(@NonNull OkHttpClient client) {
        // explicit cast, otherwise a recursive call
        return create((Call.Factory) client);
    }

    /**
     * @since 4.0.0
     */
    @NonNull
    public static OkHttpNetworkSchemeHandler create(@NonNull Call.Factory factory) {
        return new OkHttpNetworkSchemeHandler(factory);
    }

    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    // @since 4.0.0, previously just OkHttpClient
    private final Call.Factory factory;

    @SuppressWarnings("WeakerAccess")
    OkHttpNetworkSchemeHandler(@NonNull Call.Factory factory) {
        this.factory = factory;
    }

    @NonNull
    @Override
    public ImageItem handle(@NonNull String raw, @NonNull Uri uri) {

        final Request request = new Request.Builder()
                .url(raw)
                .tag(raw)
                .build();

        final Response response;
        try {
            response = factory.newCall(request).execute();
        } catch (Throwable t) {
            throw new IllegalStateException("Exception obtaining network resource: " + raw, t);
        }

        if (response == null) {
            throw new IllegalStateException("Could not obtain network response: " + raw);
        }

        final ResponseBody body = response.body();
        final InputStream inputStream = body != null
                ? body.byteStream()
                : null;

        if (inputStream == null) {
            throw new IllegalStateException("Response does not contain body: " + raw);
        }

        // important to process content-type as it can have encoding specified (which we should remove)
        final String contentType =
                NetworkSchemeHandler.contentType(response.header(HEADER_CONTENT_TYPE));

        return ImageItem.withDecodingNeeded(contentType, inputStream);
    }

    @NonNull
    @Override
    public Collection<String> supportedSchemes() {
        return Arrays.asList(
                NetworkSchemeHandler.SCHEME_HTTP,
                NetworkSchemeHandler.SCHEME_HTTPS);
    }
}
