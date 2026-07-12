package com.ismartcoding.plain.lib.html2md

internal class Rule {
    var name: String = ""
    var filter: (HtmlNode) -> Boolean
        private set
    var replacement: (String, HtmlNode) -> String
        private set
    var append: (() -> String)? = null
        private set

    constructor(filter: String, replacement: (String, HtmlNode) -> String) {
        this.filter = { el: HtmlNode -> el.nodeName() == filter }
        this.replacement = replacement
    }

    constructor(filters: Set<String>, replacement: (String, HtmlNode) -> String) {
        val availableFilters = filters.map { it.lowercase() }.toSet()
        filter = { element: HtmlNode -> availableFilters.contains(element.nodeName()) }
        this.replacement = replacement
    }

    constructor(filter: (HtmlNode) -> Boolean, replacement: (String, HtmlNode) -> String) {
        this.filter = filter
        this.replacement = replacement
    }

    constructor(filter: (HtmlNode) -> Boolean, replacement: (String, HtmlNode) -> String, append: () -> String) {
        this.filter = filter
        this.replacement = replacement
        this.append = append
    }
}
