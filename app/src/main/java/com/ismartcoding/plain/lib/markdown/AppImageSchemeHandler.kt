package com.ismartcoding.plain.lib.markdown

import android.content.Context
import android.net.Uri
import com.ismartcoding.plain.lib.extensions.getFinalPath
import com.ismartcoding.plain.lib.extensions.getMimeType
import com.ismartcoding.plain.lib.markdown.image.ImageItem
import com.ismartcoding.plain.lib.markdown.image.SchemeHandler
import java.io.FileInputStream
import java.util.*

class AppImageSchemeHandler(val context: Context) : SchemeHandler() {
    override fun handle(
        raw: String,
        uri: Uri,
    ): ImageItem {
        return ImageItem.withDecodingNeeded(raw.getMimeType(), FileInputStream(raw.getFinalPath(context)))
    }

    override fun supportedSchemes(): MutableCollection<String> {
        return Collections.singleton("app")
    }
}
