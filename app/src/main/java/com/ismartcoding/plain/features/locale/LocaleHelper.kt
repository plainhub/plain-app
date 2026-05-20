package com.ismartcoding.plain.features.locale

import androidx.annotation.PluralsRes
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.mustache.Mustache
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.i18n.mustache
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString as getComposeString
import org.jetbrains.compose.resources.getPluralString as getComposePluralString
import java.util.Locale

object LocaleHelper {
    fun currentLocale(): Locale {
        return MainApp.instance.resources.configuration.locales.get(0)
    }

    fun getString(resourceKey: Int): String {
        return try {
            MainApp.instance.resources.getString(resourceKey)
        } catch (e: Exception) {
            resourceKey.toString()
        }
    }

    fun getStringIdentifier(identifierName: String): Int {
        return MainApp.instance.resources.getIdentifier(identifierName, "string", MainApp.instance.packageName)
    }

    fun getString(identifierName: String): String {
        val identifier = MainApp.instance.resources.getIdentifier(identifierName, "string", MainApp.instance.packageName)
        return try {
            if (identifier == 0) {
                ""
            } else {
                MainApp.instance.getString(identifier)
            }
        } catch (e: Exception) {
            identifierName
        }
    }

    fun getQuantityString(
        @PluralsRes id: Int,
        quantity: Int,
    ): String {
        return MainApp.instance.resources.getQuantityString(id, quantity, quantity)
    }

    fun getStringF(
        resourceKey: Int,
        vararg formatArguments: Any,
    ): String {
        var text = ""
        return try {
            if (formatArguments.size % 2 != 0) {
                return ""
            }
            text = MainApp.instance.getString(resourceKey)
            val tmpl = Mustache.compiler().defaultValue("").compile(text)
            val params: MutableMap<String, Any> = HashMap()
            var i = 0
            while (i < formatArguments.size) {
                params[formatArguments[i].toString()] = formatArguments[i + 1]
                i += 2
            }
            tmpl.execute(params)
        } catch (e: Exception) {
            LogCat.e(e.toString())
            text
        }
    }

    // ── KMP / Compose Multiplatform Resources API ─────────────────────────────

    /**
     * Load a [StringResource] from the shared KMP module.
     * Prefer calling `stringResource(Res.string.xxx)` directly inside a Composable.
     *
     * Usage (suspend / coroutine context):
     *   val text = LocaleHelper.getString(Res.string.cancel)
     */
    suspend fun getString(resource: StringResource): String = getComposeString(resource)

    /**
     * Load a [StringResource] and apply Mustache `{{ key }}` substitution.
     *
     * Usage (suspend / coroutine context):
     *   val text = LocaleHelper.getStringF(Res.string.last_update, "time", "5 min ago")
     *   val text = LocaleHelper.getStringF(Res.string.exported_to, "name", fileName)
     *
     * Arguments are key-value pairs: key₁, value₁, key₂, value₂, …
     */
    suspend fun getStringF(resource: StringResource, vararg formatArguments: Any): String {
        if (formatArguments.size % 2 != 0) return getComposeString(resource)
        val text = getComposeString(resource)
        return text.mustache(*toMustachePairs(formatArguments))
    }

    /**
     * Load a [PluralStringResource] for [quantity] from the shared KMP module.
     *
     * Usage (suspend / coroutine context):
     *   val text = LocaleHelper.getQuantityString(Res.plurals.items, count)
     */
    suspend fun getQuantityString(resource: PluralStringResource, quantity: Int): String =
        getComposePluralString(resource, quantity, quantity)

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
