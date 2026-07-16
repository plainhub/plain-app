package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext

actual object AppResources {
    actual fun color(name: String): Int = appContext.resources.getIdentifier(name, "color", appContext.packageName)
    actual fun drawable(name: String): Int = appContext.resources.getIdentifier(name, "drawable", appContext.packageName)
    actual fun mipmap(name: String): Int = appContext.resources.getIdentifier(name, "mipmap", appContext.packageName)
}
