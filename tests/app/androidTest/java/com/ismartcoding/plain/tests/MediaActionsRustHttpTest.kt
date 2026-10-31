package com.ismartcoding.plain.tests


import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.graphql.Query
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.features.*
import com.ismartcoding.plain.ui.permissions.*
import com.ismartcoding.plain.features.chat.*
import com.ismartcoding.plain.ui.chat.*
import com.ismartcoding.plain.features.clipboard.*
import com.ismartcoding.plain.ui.clipboard.*
import com.ismartcoding.plain.features.files.*
import com.ismartcoding.plain.ui.files.*
import com.ismartcoding.plain.features.images.editor.*
import com.ismartcoding.plain.ui.images.editor.*
import com.ismartcoding.plain.features.notes.*
import com.ismartcoding.plain.ui.notes.*
import com.ismartcoding.plain.features.permissions.*
import com.ismartcoding.plain.ui.permissions.*
import com.ismartcoding.plain.features.pomodoro.*
import com.ismartcoding.plain.ui.pomodoro.*
import com.ismartcoding.plain.features.tags.*
import com.ismartcoding.plain.ui.tags.*
import com.ismartcoding.plain.ui.videos.*
import com.ismartcoding.plain.features.audio.*
import com.ismartcoding.plain.ui.audio.*
import com.ismartcoding.plain.features.media.actions.*
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.platform.media.deleteMedia
import com.ismartcoding.plain.platform.media.restoreMedia
import com.ismartcoding.plain.platform.media.trashMedia
import java.io.File
import java.nio.ByteOrder
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import android.content.ContentUris
import android.os.Bundle
import android.util.Base64

@RunWith(AndroidJUnit4::class)
class MediaActionsRustHttpTest {
    @Test
    fun physicalActionsCleanOnlyConfirmedAssetsAndPreserveFailedReferences() = runBlocking {
        assertTrue(AudioQueueClient.queuedMediaIds().isEmpty())
        assertTrue(AudioQueueClient.playback().path.isNullOrEmpty())
        val prefix = "media-actions-${UUID.randomUUID()}"
        val missingId = "9223372036854775806"
        val uris = mutableListOf<Uri>()
        val tagIds = mutableListOf<String>()
        val paths = mutableSetOf<String>()
        val videoIds = mutableListOf<String>()
        var playlistId = ""
        var moveDir: File? = null
        fun create(type: DataType): Pair<String,String> {
            val audio = type == DataType.AUDIO
            val video = type == DataType.VIDEO
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME,"$prefix.${if (audio) "wav" else if (video) "mp4" else "png"}")
                put(MediaStore.MediaColumns.MIME_TYPE,if (audio) "audio/wav" else if (video) "video/mp4" else "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH,if (audio) "Music/PlainSyntheticTests" else if (video) "Movies/PlainSyntheticTests" else "Pictures/PlainSyntheticTests")
                put(MediaStore.MediaColumns.IS_PENDING,1)
            }
            val resolver = appContext.contentResolver
            val uri = checkNotNull(resolver.insert(if (audio) MediaStore.Audio.Media.EXTERNAL_CONTENT_URI else if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values))
            uris.add(uri)
            checkNotNull(resolver.openOutputStream(uri)).use { output ->
                if (audio) {
                    val length = 8_000 * 2
                    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).put("RIFF".toByteArray()).putInt(length+36).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(8_000).putInt(16_000).putShort(2).putShort(16).put("data".toByteArray()).putInt(length).array()
                    output.write(header);output.write(ByteArray(length))
                } else if (video) {
                    output.write(Base64.decode(syntheticVideo,Base64.DEFAULT))
                } else {
                    val bitmap = Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888)
                    try { bitmap.eraseColor(0xff225588.toInt());check(bitmap.compress(Bitmap.CompressFormat.PNG,100,output)) }
                    finally { bitmap.recycle() }
                }
            }
            values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING,0)
            assertEquals(1,resolver.update(uri,values,null,null))
            val id = checkNotNull(uri.lastPathSegment)
            val path = checkNotNull(resolver.query(uri,arrayOf(MediaStore.MediaColumns.DATA),null,null,null)).use {
                assertTrue(it.moveToFirst());checkNotNull(it.getString(0))
            }
            paths.add(path)
            return id to path
        }
        suspend fun tag(type: DataType): String = TagsClient.create(type, prefix).id.also { tagIds.add(it) }
        try {
            val (id,path) = create(DataType.AUDIO)
            val audioTag = tag(DataType.AUDIO)
            val videoTag = tag(DataType.VIDEO)
            TagsClient.editTagRelations(DataType.AUDIO,listOf(id),listOf(audioTag),emptyList())
            TagsClient.editTagRelations(DataType.AUDIO,listOf(missingId),listOf(audioTag),emptyList())
            TagsClient.editTagRelations(DataType.VIDEO,listOf(id),listOf(videoTag),emptyList())
            val track = AudioItem(title = "Synthetic", artist = prefix, path = path, durationMs = 1000, mediaId = id)
            playlistId = AudioPlaylistsClient.createPlaylist(prefix).id
            AudioPlaylistsClient.addPlaylistItems(playlistId,listOf(track))
            AudioQueueClient.enqueue(listOf(track))
            assertEquals(id, checkNotNull(AudioQueueClient.resolveNext(true)).mediaId)
            var failed = false
            try { MediaActionClient.run(DataType.AUDIO,MediaAction.TRASH,listOf(id,missingId)) }
            catch (error: Exception) { failed = error.message?.contains("1 media actions failed (1 completed)") == true }
            assertTrue("Partial failure must be visible",failed)
            assertTrue(TagsClient.getTagRelationsByKeys(setOf(id), DataType.AUDIO).isEmpty())
            assertEquals(1,TagsClient.getTagRelationsByKeys(setOf(missingId), DataType.AUDIO).size)
            assertEquals(1,TagsClient.getTagRelationsByKeys(setOf(id), DataType.VIDEO).size)
            assertFalse(AudioQueueClient.queuedMediaIds().contains(id))
            assertTrue(AudioQueueClient.playback().path.isNullOrEmpty())
            assertEquals(0,checkNotNull(AudioPlaylistsClient.playlist(playlistId)).itemCount)
            assertTrue(GraphqlApi.execute(Query.mediaPlayHistory("type:AUDIO ids:=$id", 0, 20)).isEmpty())
            restoreMedia(DataType.AUDIO,setOf(id))
            assertTrue(File(path).isFile)
            deleteMedia(DataType.AUDIO,setOf(id),false)
            assertFalse(File(path).exists())
            val (videoId,videoPath) = create(DataType.VIDEO)
            videoIds.add(videoId)
            GraphqlApi.execute(Mutation.saveMediaPlayProgress(videoId, DataType.VIDEO, 5_000_000_001L))
            TagsClient.editTagRelations(DataType.VIDEO,listOf(videoId),listOf(videoTag),emptyList())
            trashMedia(DataType.VIDEO,setOf(videoId))
            assertTrue(GraphqlApi.execute(Query.mediaPlayHistory("type:VIDEO ids:=$videoId", 0, 20)).none { it.mediaId == videoId })
            assertTrue(TagsClient.getTagRelationsByKeys(setOf(videoId), DataType.VIDEO).isEmpty())
            deleteMedia(DataType.VIDEO,setOf(videoId),true)
            assertFalse(File(videoPath).exists())
            val (imageId,imagePath) = create(DataType.IMAGE)
            val imageTag = tag(DataType.IMAGE)
            TagsClient.editTagRelations(DataType.IMAGE,listOf(imageId),listOf(imageTag),emptyList())
            moveDir = File(File(imagePath).parentFile,"$prefix-moved")
            assertEquals(1,MediaActionClient.run(DataType.IMAGE,MediaAction.MOVE,listOf(imageId),destDir=checkNotNull(moveDir).path))
            val movedPath = File(moveDir,File(imagePath).name).path
            assertFalse(File(imagePath).exists())
            assertTrue(File(movedPath).isFile)
            assertEquals(1,TagsClient.getTagRelationsByKeys(setOf(imageId), DataType.IMAGE).size)
            trashMedia(DataType.IMAGE,setOf(imageId))
            assertTrue(TagsClient.getTagRelationsByKeys(setOf(imageId), DataType.IMAGE).isEmpty())
            restoreMedia(DataType.IMAGE,setOf(imageId))
            deleteMedia(DataType.IMAGE,setOf(imageId),false)
            assertFalse(File(movedPath).exists())
        } finally {
            try {
                AudioQueueClient.clearQueue()
                if (playlistId.isNotEmpty()) AudioPlaylistsClient.deletePlaylist(playlistId)
                tagIds.forEach { TagsClient.delete(it) }
                videoIds.forEach { videoId ->
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId.toLong())
                    val trashed = checkNotNull(appContext.contentResolver.query(
                        uri, arrayOf(MediaStore.MediaColumns.IS_TRASHED),
                        Bundle().apply { putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE) }, null,
                    )).use { if (it.moveToFirst()) it.getInt(0) != 0 else null }
                    if (trashed != null) deleteMedia(DataType.VIDEO, setOf(videoId), trashed)
                }
            } finally {
                try { uris.forEach { appContext.contentResolver.delete(it,Bundle().apply { putInt(MediaStore.QUERY_ARG_MATCH_TRASHED,MediaStore.MATCH_INCLUDE) }) } }
                finally { moveDir?.delete() }
            }
        }
    }
}

internal val syntheticVideo = "AAAAIGZ0eXBpc29tAAACAGlzb21pc28yYXZjMW1wNDEAAAMVbW9vdgAAAGxtdmhkAAAAAAAAAAAAAAAAAAAD6AAAA+gAAQAAAQAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAkB0cmFrAAAAXHRraGQAAAADAAAAAAAAAAAAAAABAAAAAAAAA+gAAAAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAABAAAAAAEAAAABAAAAAAAAkZWR0cwAAABxlbHN0AAAAAAAAAAEAAAPoAAAAAAABAAAAAAG4bWRpYQAAACBtZGhkAAAAAAAAAAAAAAAAAABAAAAAQABVxAAAAAAALWhkbHIAAAAAAAAAAHZpZGUAAAAAAAAAAAAAAABWaWRlb0hhbmRsZXIAAAABY21pbmYAAAAUdm1oZAAAAAEAAAAAAAAAAAAAACRkaW5mAAAAHGRyZWYAAAAAAAAAAQAAAAx1cmwgAAAAAQAAASNzdGJsAAAAv3N0c2QAAAAAAAAAAQAAAK9hdmMxAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAEAAQABIAAAASAAAAAAAAAABFExhdmM2My4xLjEwMSBsaWJ4MjY0AAAAAAAAAAAAAAAAGP//AAAANWF2Y0MBZAAK/+EAGGdkAAqs2UQmwEQAAAMABAAAAwAIPEiWWAEABmjr48siwP34+AAAAAAQcGFzcAAAAAEAAAABAAAAFGJ0cnQAAAAAAAAWuAAAAAAAAAAYc3R0cwAAAAAAAAABAAAAAQAAQAAAAAAcc3RzYwAAAAAAAAABAAAAAQAAAAEAAAABAAAAFHN0c3oAAAAAAAAC1wAAAAEAAAAUc3RjbwAAAAAAAAABAAADRQAAAGF1ZHRhAAAAWW1ldGEAAAAAAAAAIWhkbHIAAAAAAAAAAG1kaXJhcHBsAAAAAAAAAAAAAAAALGlsc3QAAAAkqXRvbwAAABxkYXRhAAAAAQAAAABMYXZmNjMuMS4xMDEAAAAIZnJlZQAAAt9tZGF0AAACrQYF//+p3EXpvebZSLeWLNgg2SPu73gyNjQgLSBjb3JlIDE2NSByMzIyMiBiMzU2MDVhIC0gSC4yNjQvTVBFRy00IEFWQyBjb2RlYyAtIENvcHlsZWZ0IDIwMDMtMjAyNSAtIGh0dHA6Ly93d3cudmlkZW9sYW4ub3JnL3gyNjQuaHRtbCAtIG9wdGlvbnM6IGNhYmFjPTEgcmVmPTMgZGVibG9jaz0xOjA6MCBhbmFseXNlPTB4MzoweDExMyBtZT1oZXggc3VibWU9NyBwc3k9MSBwc3lfcmQ9MS4wMDowLjAwIG1peGVkX3JlZj0xIG1lX3JhbmdlPTE2IGNocm9tYV9tZT0xIHRyZWxsaXM9MSA4eDhkY3Q9MSBjcW09MCBkZWFkem9uZT0yMSwxMSBmYXN0X3Bza2lwPTEgY2hyb21hX3FwX29mZnNldD0tMiB0aHJlYWRzPTIgbG9va2FoZWFkX3RocmVhZHM9MSBzbGljZWRfdGhyZWFkcz0wIG5yPTAgZGVjaW1hdGU9MSBpbnRlcmxhY2VkPTAgYmx1cmF5X2NvbXBhdD0wIGNvbnN0cmFpbmVkX2ludHJhPTAgYmZyYW1lcz0zIGJfcHlyYW1pZD0yIGJfYWRhcHQ9MSBiX2JpYXM9MCBkaXJlY3Q9MSB3ZWlnaHRiPTEgb3Blbl9nb3A9MCB3ZWlnaHRwPTIga2V5aW50PTI1MCBrZXlpbnRfbWluPTEgc2NlbmVjdXQ9NDAgaW50cmFfcmVmcmVzaD0wIHJjX2xvb2thaGVhZD00MCByYz1jcmYgbWJ0cmVlPTEgY3JmPTIzLjAgcWNvbXA9MC42MCBxcG1pbj0wIHFwbWF4PTY5IHFwc3RlcD00IGlwX3JhdGlvPTEuNDAgYXE9MToxLjAwAIAAAAAiZYiEABX//vfJ78Cm69vetb+Tz0j4e8ZQD6wZq+vbSH/7MQ=="
