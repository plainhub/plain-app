package com.ismartcoding.plain.ui.helpers

import com.ismartcoding.plain.appContext

object AppResources {
    fun color(name: String): Int = appContext.resources.getIdentifier(name, "color", appContext.packageName)
    fun drawable(name: String): Int = appContext.resources.getIdentifier(name, "drawable", appContext.packageName)
    fun mipmap(name: String): Int = appContext.resources.getIdentifier(name, "mipmap", appContext.packageName)
}
