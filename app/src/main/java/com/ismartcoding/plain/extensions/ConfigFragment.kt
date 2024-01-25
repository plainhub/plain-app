package com.ismartcoding.plain.extensions

import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.helpers.JsonHelper.jsonDecode
import com.ismartcoding.plain.features.route.Route
import com.ismartcoding.plain.features.rule.Rule
import com.ismartcoding.plain.fragment.ConfigFragment
import kotlinx.serialization.json.Json

fun ConfigFragment.toRule(): Rule {
    val r = jsonDecode<Rule>(value)
    r.id = id
    return r
}

fun ConfigFragment.toRoute(): Route {
    val r = jsonDecode<Route>(value)
    r.id = id
    return r
}
