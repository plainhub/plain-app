package com.ismartcoding.lib.markdown.image.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class DataUriParser {

    @Nullable
    public abstract DataUri parse(@NonNull String input);


    @NonNull
    public static DataUriParser create() {
        return new Impl();
    }

    static class Impl extends DataUriParser {

        @Nullable
        @Override
        public DataUri parse(@NonNull String input) {

            final int index = input.indexOf(',');
            // we expect exactly one comma
            if (index < 0) {
                return null;
            }

            final String contentType;
            final boolean base64;

            if (index > 0) {
                final String part = input.substring(0, index);
                final String[] parts = part.split(";");
                final int length = parts.length;
                if (length > 0) {
                    // if one: either content-type or base64
                    if (length == 1) {
                        final String value = parts[0];
                        if ("base64".equals(value)) {
                            contentType = null;
                            base64 = true;
                        } else {
                            contentType = value.indexOf('/') > -1
                                    ? value
                                    : null;
                            base64 = false;
                        }
                    } else {
                        contentType = parts[0].indexOf('/') > -1
                                ? parts[0]
                                : null;
                        base64 = "base64".equals(parts[length - 1]);
                    }
                } else {
                    contentType = null;
                    base64 = false;
                }
            } else {
                contentType = null;
                base64 = false;
            }

            final String data;
            if (index < input.length()) {
                final String value = input.substring(index + 1, input.length()).replaceAll("\n", "");
                if (value.length() == 0) {
                    data = null;
                } else {
                    data = value;
                }
            } else {
                data = null;
            }

            return new DataUri(contentType, base64, data);
        }
    }
}
