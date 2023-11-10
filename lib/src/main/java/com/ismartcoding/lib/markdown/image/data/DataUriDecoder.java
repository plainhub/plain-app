package com.ismartcoding.lib.markdown.image.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;

public abstract class DataUriDecoder {

    @Nullable
    public abstract byte[] decode(@NonNull DataUri dataUri) throws Throwable;

    @NonNull
    public static DataUriDecoder create() {
        return new Impl();
    }

    static class Impl extends DataUriDecoder {

        private static final String CHARSET = "UTF-8";

        @Nullable
        @Override
        public byte[] decode(@NonNull DataUri dataUri) throws Throwable {

            final String data = dataUri.data();

            if (!TextUtils.isEmpty(data)) {
                if (dataUri.base64()) {
                    return Base64.decode(data.getBytes(CHARSET), Base64.DEFAULT);
                } else {
                    return data.getBytes(CHARSET);
                }
            } else {
                return null;
            }
        }
    }
}
