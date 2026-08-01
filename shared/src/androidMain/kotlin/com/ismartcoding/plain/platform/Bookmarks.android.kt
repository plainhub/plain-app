package com.ismartcoding.plain.platform

import android.os.Environment
import com.ismartcoding.plain.appContext
import java.io.File

actual fun bookmarkFaviconDirPath(): String {
    val base = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        ?: File(appDir(), Environment.DIRECTORY_PICTURES)
    return File(base, "bookmark_favicons").absolutePath
}
