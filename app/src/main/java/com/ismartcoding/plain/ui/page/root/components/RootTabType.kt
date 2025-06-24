package com.ismartcoding.plain.ui.page.root.components


enum class RootTabType(val value: Int) {
    HOME(0),
    CHAT(1),
    AUDIO(2),
    IMAGES(3),
    VIDEOS(4);

    companion object {
        fun fromValue(value: Int): RootTabType {
            return entries.find { it.value == value } ?: HOME
        }
    }
}