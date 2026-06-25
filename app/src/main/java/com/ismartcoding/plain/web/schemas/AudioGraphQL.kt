package com.ismartcoding.plain.web.schemas
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.events.ClearAudioPlaylistEvent
import com.ismartcoding.plain.audio.AudioPlayer
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.audio.AudioMediaStoreHelper
import com.ismartcoding.plain.preferences.AudioPlayModePreference
import com.ismartcoding.plain.preferences.AudioPlayingPreference
import com.ismartcoding.plain.preferences.AudioPlaylistPreference
import com.ismartcoding.plain.preferences.AudioSortByPreference
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.Audio
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addAudioSchema() {
    query("audios") {
        configure {
            executor = Executor.DataLoaderPrepared
        }
        resolver { offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
            val context = MainApp.instance
            Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
            AudioMediaStoreHelper.searchAsync(context, query, limit, offset, sortBy).map { it.toModel() }
        }
        type<Audio> {
            dataProperty("tags") {
                prepare { item -> item.id.value }
                loader { ids ->
                    TagsLoader.load(ids, DataType.AUDIO)
                }
            }
        }
    }
    query("audioCount") {
        resolver { query: String ->
            if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndCanAsync(MainApp.instance)) {
                AudioMediaStoreHelper.countAsync(MainApp.instance, query)
            } else {
                0
            }
        }
    }
    mutation("playAudio") {
        resolver { path: String ->
            val context = MainApp.instance
            val audio = DPlaylistAudio.fromPath(context, path)
            AudioPlayingPreference.putAsync(audio.path)
            if (!AudioPlaylistPreference.getValueAsync().any { it.path == audio.path }) {
                AudioPlaylistPreference.addAsync(listOf(audio))
            }
            audio.toModel()
        }
    }
    mutation("updateAudioPlayMode") {
        resolver { mode: MediaPlayMode ->
            AudioPlayModePreference.putAsync(mode)
            true
        }
    }
    mutation("clearAudioPlaylist") {
        resolver { ->
            val context = MainApp.instance
            AudioPlayingPreference.putAsync("")
            AudioPlaylistPreference.putAsync(arrayListOf())
            coMain {
                AudioPlayer.clear()
            }
            sendEvent(ClearAudioPlaylistEvent())
            true
        }
    }
    mutation("deletePlaylistAudio") {
        resolver { path: String ->
            AudioPlaylistPreference.deleteAsync(setOf(path))
            true
        }
    }
    mutation("addPlaylistAudios") {
        resolver { query: String ->
            val context = MainApp.instance
            // 1000 items at most
            val items = AudioMediaStoreHelper.searchAsync(context, query, 1000, 0, AudioSortByPreference.getValueAsync())
            AudioPlaylistPreference.addAsync(items.map { it.toPlaylistAudio() })
            true
        }
    }
    mutation("reorderPlaylistAudios") {
        resolver { paths: List<String> ->
            val context = MainApp.instance

            // Get current playlist
            val currentPlaylist = AudioPlaylistPreference.getValueAsync()
            if (currentPlaylist.isEmpty() || paths.isEmpty()) {
                return@resolver true
            }

            // Create a map of paths to audio items
            val audioMap = currentPlaylist.associateBy { it.path }

            // Reorder the playlist based on the provided paths
            val reorderedPlaylist = mutableListOf<DPlaylistAudio>()

            // First add audio items in the new order
            paths.forEach { path ->
                audioMap[path]?.let { audio ->
                    reorderedPlaylist.add(audio)
                }
            }

            // Add other audio items that are not in the reorder list (keep their original positions)
            currentPlaylist.forEach { audio ->
                if (!paths.contains(audio.path)) {
                    reorderedPlaylist.add(audio)
                }
            }

            // Save the reordered playlist
            AudioPlaylistPreference.putAsync(reorderedPlaylist)

            true
        }
    }
}
