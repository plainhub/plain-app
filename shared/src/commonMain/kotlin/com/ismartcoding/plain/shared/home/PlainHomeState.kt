package com.ismartcoding.plain.shared.home

data class PlainHomeShortcut(
    val id: String,
    val title: String,
    val subtitle: String,
)

data class PlainHomeState(
    val deviceName: String,
    val webAddress: String,
    val shortcuts: List<PlainHomeShortcut>,
)

class PlainHomeStateProvider {
    private val state = PlainHomeState(
        deviceName = "PlainApp",
        webAddress = "http://127.0.0.1:8080",
        shortcuts = listOf(
            PlainHomeShortcut("images", "Images", "Browse photos"),
            PlainHomeShortcut("files", "Files", "Manage local files"),
            PlainHomeShortcut("docs", "Docs", "Read documents"),
            PlainHomeShortcut("notes", "Notes", "Capture ideas"),
            PlainHomeShortcut("feeds", "Feeds", "Follow updates"),
            PlainHomeShortcut("chat", "Chat", "Send messages"),
            PlainHomeShortcut("audio", "Audio", "Play music"),
            PlainHomeShortcut("videos", "Videos", "Watch videos"),
        ),
    )

    fun getState(): PlainHomeState {
        return state
    }

    fun getShortcutCount(): Int {
        return state.shortcuts.size
    }

    fun getShortcut(index: Int): PlainHomeShortcut {
        return state.shortcuts[index]
    }
}
