package com.ismartcoding.plain.features.dlna

sealed class DlnaCommand {
    data class SetUri(val uri: String, val title: String = "") : DlnaCommand()
    data object Play : DlnaCommand()
    data object Pause : DlnaCommand()
    data object Stop : DlnaCommand()
    data class Seek(val positionMs: Long) : DlnaCommand()
}
