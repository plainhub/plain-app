package com.ismartcoding.plain.features.locale

import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.i18n.mustache
import org.jetbrains.compose.resources.StringResource
import java.util.Locale
import org.jetbrains.compose.resources.getString as getComposeString

object LocaleHelper {
    fun currentLocale(): Locale {
        return MainApp.instance.resources.configuration.locales.get(0)
    }

    suspend fun getStringAsync(resource: StringResource): String = getComposeString(resource)

    suspend fun getStringFAsync(resource: StringResource, vararg formatArguments: Any): String {
        if (formatArguments.size % 2 != 0) return getComposeString(resource)
        val text = getComposeString(resource)
        return text.mustache(*toMustachePairs(formatArguments))
    }

    fun getString(resource: StringResource): String = kotlinx.coroutines.runBlocking { getComposeString(resource) }

    fun getStringF(resource: StringResource, vararg formatArguments: Any): String {
        if (formatArguments.size % 2 != 0) return getString(resource)
        val text = getString(resource)
        return text.mustache(*toMustachePairs(formatArguments))
    }

    private fun toMustachePairs(args: Array<out Any>): Array<Pair<String, Any>> {
        val result = ArrayList<Pair<String, Any>>(args.size / 2)
        var i = 0
        while (i < args.size) {
            result.add(args[i].toString() to args[i + 1])
            i += 2
        }
        return result.toTypedArray()
    }
}
