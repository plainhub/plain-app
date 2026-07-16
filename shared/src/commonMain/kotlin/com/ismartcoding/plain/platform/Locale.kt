package com.ismartcoding.plain.platform

import com.ismartcoding.plain.i18n.mustache
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString as getComposeString

data class Locale(
    val language: String,
    val country: String,
) {
    val isZhCN: Boolean get() = language == "zh" && country == "CN"

    /**
     * Human-readable display name with elegant handling for Chinese variants.
     */
    fun getElegantDisplayName(): String = when {
        language == "zh" && country == "TW" -> "繁體中文"
        language == "zh" && country == "CN" -> "简体中文"
        else -> getLocaleDisplayName(this)
    }
}

expect fun currentLocale(): Locale

/**
 * Platform-specific display name for a locale (e.g. "English (United States)").
 */
expect fun getLocaleDisplayName(locale: Locale): String

/**
 * Apply the given locale to the system (Android resources + JVM default).
 * On iOS this is a no-op (handled via system locale).
 */
expect fun setSystemLocale(locale: Locale?)

object LocaleHelper {
    fun currentLocale(): Locale = com.ismartcoding.plain.platform.currentLocale()

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
