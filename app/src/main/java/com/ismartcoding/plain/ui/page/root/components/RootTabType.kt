package com.ismartcoding.plain.ui.page.root.components


enum class RootTabType(val value: Int) {
    HOME(0),
    AUDIO(1),
    IMAGES(2),
    VIDEOS(3);

    companion object {
        fun fromValue(value: Int): RootTabType {
            return entries.find { it.value == value } ?: HOME
        }
    }
}