package com.ismartcoding.lib.opml.entity

class Outline(val attributes: Map<String, String>, val subElements: List<Outline> = listOf()) {
    fun getName(): String {
        return attributes["title"] ?: attributes["text"] ?: ""
    }

    fun getUrl(): String {
        return attributes["xmlUrl"] ?: ""
    }
}
