package com.ismartcoding.plain

import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.lib.serialize.serialLazy
import com.ismartcoding.plain.features.audio.DPlaylistAudio
import com.ismartcoding.plain.features.audio.MediaPlayMode
import com.ismartcoding.plain.features.device.DeviceSortBy
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.video.DVideo

object LocalStorage {
    var clientId: String by serialLazy("")
    var fileIdToken: String by serialLazy("") // use to generate file path to id or decrypt file id to path

    var deviceSortBy: DeviceSortBy by serialLazy(DeviceSortBy.LAST_ACTIVE)
    var audioPlayMode: MediaPlayMode by serialLazy(MediaPlayMode.REPEAT)
    var audioPlaylist: List<DPlaylistAudio> by serialLazy(listOf())
    var audioPlaying: DPlaylistAudio? by serialLazy(null)
    var audioSortBy: FileSortBy by serialLazy(FileSortBy.DATE_DESC)
    var audioSleepTimerMinutes: Int by serialLazy(30)
    var audioSleepTimerFutureTime: Long by serialLazy(0)
    var audioSleepTimerFinishAudio: Boolean by serialLazy(false)
    var videoSortBy: FileSortBy by serialLazy(FileSortBy.DATE_DESC)
    var videoPlayerList: List<DVideo> by serialLazy(listOf())
    var imageSortBy: FileSortBy by serialLazy(FileSortBy.DATE_DESC)
    var fileSortBy: FileSortBy by serialLazy(FileSortBy.NAME_ASC)
    var showHiddenFiles: Boolean by serialLazy(false)
    var editorAccessoryLevel: Int by serialLazy(0)
    var editorWrapContent: Boolean by serialLazy(true)
    var editorShowLineNumbers: Boolean by serialLazy(true)
    var editorSyntaxHighlight: Boolean by serialLazy(true)
    var scanResults: List<String> by serialLazy(listOf())
    var noteIsEditMode: Boolean by serialLazy(true)
    var feedAutoRefresh: Boolean by serialLazy(true)
    var feedAutoRefreshInterval: Int by serialLazy(7200) // seconds
    var feedAutoRefreshOnlyWifi: Boolean by serialLazy(false)
    var chatGPTApiKey: String by serialLazy("")


    fun deletePlaylistAudio(path: String) {
        audioPlaylist = audioPlaylist.toMutableList().apply {
            removeIf { it.path == path }
        }
    }

    fun deletePlaylistAudios(paths: Set<String>) {
        audioPlaylist = audioPlaylist.toMutableList().apply {
            removeIf { paths.contains(it.path) }
        }
    }

    fun addPlaylistAudio(audio: DPlaylistAudio) {
        if (!audioPlaylist.any { it.path == audio.path }) {
            audioPlaylist = audioPlaylist.toMutableList().apply {
                add(audio)
            }
        }
    }

    fun addPlaylistAudios(audios: List<DPlaylistAudio>) {
        val items = audioPlaylist.toMutableList()
        items.removeIf { i -> audios.any { it.path == i.path } }
        items.addAll(audios)
        audioPlaylist = items
    }

    fun deleteVideo(path: String) {
        videoPlayerList = videoPlayerList.toMutableList().apply {
            removeIf { it.path == path }
        }
    }

    fun deleteVideos(paths: Set<String>) {
        videoPlayerList = videoPlayerList.toMutableList().apply {
            removeIf { paths.contains(it.path) }
        }
    }

    fun addVideo(video: DVideo) {
        if (!videoPlayerList.any { it.path == video.path }) {
            videoPlayerList = videoPlayerList.toMutableList().apply {
                add(video)
            }
        }
    }

    fun addVideos(videos: List<DVideo>) {
        val items = videoPlayerList.toMutableList()
        items.removeIf { i -> videos.any { it.path == i.path } }
        items.addAll(videos)
        videoPlayerList = items
    }

    fun init() {
        if (clientId.isEmpty()) {
            clientId = StringHelper.shortUUID()
        }
        fileIdToken = CryptoHelper.generateAESKey()
    }
}