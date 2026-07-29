package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext

actual fun appResourceColor(name: String): Int =
    appContext.resources.getIdentifier(name, "color", appContext.packageName)

actual fun appResourceDrawable(name: String): Int =
    appContext.resources.getIdentifier(name, "drawable", appContext.packageName)

actual fun appResourceMipmap(name: String): Int =
    appContext.resources.getIdentifier(name, "mipmap", appContext.packageName)
