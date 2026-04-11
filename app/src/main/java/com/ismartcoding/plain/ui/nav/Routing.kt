package com.ismartcoding.plain.ui.nav

import kotlinx.serialization.Serializable

class Routing {
    @Serializable
    object Home

    @Serializable
    object Settings

    @Serializable
    object Language

    @Serializable
    object DarkTheme

    @Serializable
    object BackupRestore

    @Serializable
    object WebSettings

    @Serializable
    object CustomFeatures

    @Serializable
    object NotificationSettings

    @Serializable
    object WebSecurity

    @Serializable
    object WebLearnMore

    @Serializable
    data class Text(val title: String, val content: String, val language: String)

    @Serializable
    object Sessions

    @Serializable
    object WebDev

    @Serializable
    object ExchangeRate

    @Serializable
    object SoundMeter

    @Serializable
    object PomodoroTimer

    @Serializable
    data class Chat(val id: String = "")

    @Serializable
    data class ChatInfo(val chatId: String)

    @Serializable
    data class ChatText(val content: String)

    @Serializable
    data class ChatEditText(val id: String, val content: String)

    @Serializable
    object Scan

    @Serializable
    object ScanHistory

    @Serializable
    object Apps

    @Serializable
    object Images

    @Serializable
    object Videos

    @Serializable
    object Audio

    @Serializable
    object ChatList

    @Serializable
    data class OtherFile(val path: String, val title: String)

    @Serializable
    object Docs

    @Serializable
    object Notes

    @Serializable
    data class NotesCreate(val tagId: String)

    @Serializable
    data class NoteDetail(val id: String)

    @Serializable
    data class PdfViewer(val uri: String, val fileName: String = "")

    @Serializable
    object Feeds

    @Serializable
    data class FeedEntries(val feedId: String)

    @Serializable
    data class FeedEntry(val id: String)

    @Serializable
    object FeedSettings

    @Serializable
    object AudioPlayer

    @Serializable
    data class TextFile(val path: String, val title: String = "", val mediaId: String = "", val type: String = "")

    @Serializable
    data class AppDetails(val id: String)
    
    @Serializable
    data class Files(val folderPath: String = "")

    @Serializable
    object AppFiles

    @Serializable
    data class Nearby(val pairDeviceJson: String = "")

    @Serializable
    object ComponentShowcase
}