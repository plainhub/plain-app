package com.ismartcoding.plain.i18n

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Replaces Mustache-style `{{ key }}` and `{{key}}` placeholders in a string.
 *
 * Usage:
 *   "Hello {{ name }}".mustache("name" to "World")          // → "Hello World"
 *   "{{count}} items left".mustache("count" to 3)           // → "3 items left"
 *   "Updated {{time}}".mustache("time" to "5 min ago")      // → "Updated 5 min ago"
 */
fun String.mustache(vararg args: Pair<String, Any>): String {
    if (args.isEmpty()) return this
    var result = this
    for ((key, value) in args) {
        val v = value.toString()
        result = result
            .replace("{{ $key }}", v)
            .replace("{{$key}}", v)
            .replace("{{ $key}}", v)
            .replace("{{$key }}", v)
    }
    return result
}

/**
 * Loads a [StringResource] and applies Mustache `{{ key }}` substitution.
 *
 * Usage in a Composable:
 *   stringRes(Res.string.last_update, "time" to "5 min ago")
 *   stringRes(Res.string.exported_to, "name" to fileName)
 */
@Composable
fun stringRes(resource: StringResource, vararg args: Pair<String, Any>): String {
    val raw = stringResource(resource)
    return if (args.isEmpty()) raw else raw.mustache(*args)
}

/**
 * Loads a plural [StringResource] and applies Mustache `{{ key }}` substitution.
 *
 * Usage in a Composable:
 *   pluralRes(Res.plurals.items, count, "count" to count)
 */
@Composable
fun pluralRes(resource: org.jetbrains.compose.resources.PluralStringResource, quantity: Int, vararg args: Pair<String, Any>): String {
    val raw = pluralStringResource(resource, quantity)
    return if (args.isEmpty()) raw else raw.mustache(*args)
}
