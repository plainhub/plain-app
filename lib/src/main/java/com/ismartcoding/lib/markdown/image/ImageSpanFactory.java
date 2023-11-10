package com.ismartcoding.lib.markdown.image;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.RenderProps;
import io.noties.markwon.SpanFactory;
import io.noties.markwon.image.AsyncDrawable;
import io.noties.markwon.image.AsyncDrawableSpan;
import io.noties.markwon.image.ImageProps;

public class ImageSpanFactory implements SpanFactory {
    @Nullable
    @Override
    public Object getSpans(@NonNull MarkwonConfiguration configuration, @NonNull RenderProps props) {
        return new ImageAsyncDrawableSpan(
                configuration.theme(),
                new AsyncDrawable(
                        ImageProps.DESTINATION.require(props),
                        configuration.asyncDrawableLoader(),
                        configuration.imageSizeResolver(),
                        ImageProps.IMAGE_SIZE.get(props)
                ),
                AsyncDrawableSpan.ALIGN_CENTER,
                ImageProps.REPLACEMENT_TEXT_IS_LINK.get(props, false)
        );
    }
}
