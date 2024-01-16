package com.ismartcoding.plain.features.locale

import androidx.annotation.PluralsRes
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.mustache.Mustache
import com.ismartcoding.plain.MainApp
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
}
