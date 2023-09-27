package com.ismartcoding.lib.html2md

import org.jsoup.nodes.Node
import java.util.*
import java.util.function.BiFunction
import java.util.function.Predicate
import java.util.function.Supplier

internal class Rule {
    var name: String = ""
    var filter: Predicate<Node>
        private set
    var replacement: BiFunction<String, Node, String>
        private set
    var append: Supplier<String>? = null
        private set

    constructor(filter: String, replacement: BiFunction<String, Node, String>) {
        this.filter = Predicate { el: Node -> el.nodeName().lowercase(Locale.getDefault()) == filter }
        this.replacement = replacement
    }

    constructor(filters: Set<String>, replacement: BiFunction<String, Node, String>) {
        val availableFilters = filters.toSet()
        filter = Predicate { element: Node -> availableFilters.contains(element.nodeName()) }
        this.replacement = replacement
    }

    constructor(filter: Predicate<Node>, replacement: BiFunction<String, Node, String>) {
        this.filter = filter
        this.replacement = replacement
    }

    constructor(filter: Predicate<Node>, replacement: BiFunction<String, Node, String>, append: Supplier<String>) {
        this.filter = filter
        this.replacement = replacement
        this.append = append
    }
}
