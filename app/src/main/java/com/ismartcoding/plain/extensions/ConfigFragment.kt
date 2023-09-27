package com.ismartcoding.plain.extensions

import com.ismartcoding.plain.features.route.Route
import com.ismartcoding.plain.features.rule.Rule
import com.ismartcoding.plain.fragment.ConfigFragment
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

fun ConfigFragment.toRule(): Rule {
    val r = Json.decodeFromString<Rule>(value)
    r.id = id
    return r
}

fun ConfigFragment.toRoute(): Route {
    val r = Json.decodeFromString<Route>(value)
    r.id = id
    return r
}
