package com.ismartcoding.lib.markdown.image.data;

import androidx.annotation.Nullable;

public class DataUri {

    private final String contentType;
    private final boolean base64;
    private final String data;

    public DataUri(@Nullable String contentType, boolean base64, @Nullable String data) {
        this.contentType = contentType;
        this.base64 = base64;
        this.data = data;
    }

    @Nullable
    public String contentType() {
        return contentType;
    }

    public boolean base64() {
        return base64;
    }

    @Nullable
    public String data() {
        return data;
    }

    @Override
    public String toString() {
        return "DataUri{" +
                "contentType='" + contentType + '\'' +
                ", base64=" + base64 +
                ", data='" + data + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataUri dataUri = (DataUri) o;

        if (base64 != dataUri.base64) return false;
        if (contentType != null ? !contentType.equals(dataUri.contentType) : dataUri.contentType != null)
            return false;
        return data != null ? data.equals(dataUri.data) : dataUri.data == null;
    }

    @Override
    public int hashCode() {
        int result = contentType != null ? contentType.hashCode() : 0;
        result = 31 * result + (base64 ? 1 : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }
}
