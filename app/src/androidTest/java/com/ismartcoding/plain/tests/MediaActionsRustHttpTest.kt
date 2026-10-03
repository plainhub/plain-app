package com.ismartcoding.plain.tests

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.db.DImageEmbedding
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.*
import com.ismartcoding.plain.features.audio.*
import com.ismartcoding.plain.features.mediaactions.*
import com.ismartcoding.plain.platform.getMediaItemUriString
import com.ismartcoding.plain.platform.trashMedia
import com.ismartcoding.plain.platform.restoreMedia
import com.ismartcoding.plain.platform.deleteMedia
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MediaActionsRustHttpTest {
    @Test
    fun physicalActionsCleanOnlyConfirmedAssetsAndPreserveFailedReferences() = runBlocking {
        assertTrue(AudioQueueManager.queuedPaths().isEmpty())
        assertTrue(AudioQueueManager.source().currentPath.isEmpty())
        val prefix = "media-actions-${UUID.randomUUID()}"
        val missingId = "9223372036854775806"
        val uris = mutableListOf<Uri>()
        val tagIds = mutableListOf<String>()
        val paths = mutableSetOf<String>()
        val touchedIds = mutableListOf<String>()
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
                    output.write(android.util.Base64.decode(syntheticVideo,android.util.Base64.DEFAULT))
                } else {
                    val bitmap = Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888)
                    try { bitmap.eraseColor(0xff225588.toInt());check(bitmap.compress(Bitmap.CompressFormat.PNG,100,output)) }
                    finally { bitmap.recycle() }
                }
            }
            values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING,0)
            assertEquals(1,resolver.update(uri,values,null,null))
            val id = checkNotNull(uri.lastPathSegment)
            touchedIds.add(id)
            val path = checkNotNull(resolver.query(uri,arrayOf(MediaStore.MediaColumns.DATA),null,null,null)).use {
                assertTrue(it.moveToFirst());checkNotNull(it.getString(0))
            }
            paths.add(path)
            return id to path
        }
        suspend fun tag(type: DataType): String = TagHelper.addOrUpdate("") { name = prefix; this.type = type.value }.also { tagIds.add(it) }
        try {
            val (id,path) = create(DataType.AUDIO)
            val audioTag = tag(DataType.AUDIO)
            val videoTag = tag(DataType.VIDEO)
            TagHelper.editTagRelations(DataType.AUDIO,TagRelationStub(id,"Synthetic",1),listOf(audioTag),emptyList())
            TagHelper.editTagRelations(DataType.AUDIO,TagRelationStub(missingId,"Synthetic missing",1),listOf(audioTag),emptyList())
            TagHelper.editTagRelations(DataType.VIDEO,TagRelationStub(id,"Other domain",1),listOf(videoTag),emptyList())
            MediaDurationHelper.save("audio",id,1000)
            val track = DPlaylistAudio("Synthetic",path,prefix,1000,"")
            playlistId = AudioPlaylistManager.createPlaylist(prefix).id
            AudioPlaylistManager.addPlaylistItems(playlistId,listOf(track))
            AudioQueueManager.enqueue(listOf(track))
            AudioQueueManager.setCurrent(path)
            AudioQueueManager.onPlaying(path,track.title,prefix,1000)
            var failed = false
            try { MediaActionHelper.run(DataType.AUDIO,MediaAction.TRASH,listOf(id,missingId)) }
            catch (error: Exception) { failed = error.message?.contains("1 media actions failed (1 completed)") == true }
            assertTrue("Partial failure must be visible",failed)
            assertTrue(TagHelper.getTagRelationsByKey(id,DataType.AUDIO).isEmpty())
            assertEquals(1,TagHelper.getTagRelationsByKey(missingId,DataType.AUDIO).size)
            assertEquals(1,TagHelper.getTagRelationsByKey(id,DataType.VIDEO).size)
            assertFalse(AudioQueueManager.queuedPaths().contains(path))
            assertEquals("",AudioQueueManager.source().currentPath)
            assertEquals(0,AudioPlaylistManager.playlistItemCount(playlistId))
            assertTrue(AudioPlayHistoryManager.recentPageFiltered(prefix,20,0).isEmpty())
            restoreMedia(DataType.AUDIO,setOf(id))
            assertTrue(File(path).isFile)
            deleteMedia(DataType.AUDIO,setOf(id),false)
            assertFalse(File(path).exists())
            val (videoId,videoPath) = create(DataType.VIDEO)
            videoIds.add(videoId)
            VideoProgressHelper.saveAsync(videoId,5_000_000_001L)
            MediaDurationHelper.save("video",videoId,6_000_000_001L)
            TagHelper.editTagRelations(DataType.VIDEO,TagRelationStub(videoId,"Synthetic video",1),listOf(videoTag),emptyList())
            trashMedia(DataType.VIDEO,setOf(videoId))
            assertNull(VideoProgressHelper.getAsync(videoId))
            assertTrue(MediaDurationHelper.all().none { it.mediaType == "video" && it.mediaId == videoId })
            assertTrue(TagHelper.getTagRelationsByKey(videoId,DataType.VIDEO).isEmpty())
            deleteMedia(DataType.VIDEO,setOf(videoId),true)
            assertFalse(File(videoPath).exists())
            val (imageId,imagePath) = create(DataType.IMAGE)
            val imageTag = tag(DataType.IMAGE)
            TagHelper.editTagRelations(DataType.IMAGE,TagRelationStub(imageId,"Synthetic image",1),listOf(imageTag),emptyList())
            ImageEmbeddingHelper.insertAll(listOf(DImageEmbedding(imageId,imagePath,ByteBuffer.allocate(512*4).apply { putFloat(1f) }.array())))
            moveDir = File(File(imagePath).parentFile,"$prefix-moved")
            assertEquals(1,MediaActionHelper.run(DataType.IMAGE,MediaAction.MOVE,listOf(imageId),destDir=checkNotNull(moveDir).path))
            val movedPath = File(moveDir,File(imagePath).name).path
            assertFalse(File(imagePath).exists())
            assertTrue(File(movedPath).isFile)
            assertEquals(1,TagHelper.getTagRelationsByKey(imageId,DataType.IMAGE).size)
            assertFalse(ImageEmbeddingHelper.getAllIds().contains(imageId))
            trashMedia(DataType.IMAGE,setOf(imageId))
            assertFalse(ImageEmbeddingHelper.getAllIds().contains(imageId))
            assertTrue(TagHelper.getTagRelationsByKey(imageId,DataType.IMAGE).isEmpty())
            restoreMedia(DataType.IMAGE,setOf(imageId))
            deleteMedia(DataType.IMAGE,setOf(imageId),false)
            assertFalse(File(movedPath).exists())
        } finally {
            try {
                AudioQueueManager.clearQueue()
                AudioQueueManager.removePaths(paths)
                if (playlistId.isNotEmpty()) AudioPlaylistManager.deletePlaylist(playlistId)
                tagIds.forEach { TagHelper.delete(it) }
                MediaDurationHelper.delete(DataType.AUDIO,touchedIds)
                MediaDurationHelper.delete(DataType.VIDEO,videoIds)
                videoIds.forEach { VideoProgressHelper.deleteAsync(it) }
                ImageEmbeddingHelper.deleteByIds(touchedIds)
            } finally {
                try { uris.forEach { appContext.contentResolver.delete(it,android.os.Bundle().apply { putInt(MediaStore.QUERY_ARG_MATCH_TRASHED,MediaStore.MATCH_INCLUDE) }) } }
                finally { moveDir?.delete() }
            }
        }
    }
    private val syntheticVideo = "AAAAIGZ0eXBpc29tAAACAGlzb21pc28yYXZjMW1wNDEAAAMVbW9vdgAAAGxtdmhkAAAAAAAAAAAAAAAAAAAD6AAAA+gAAQAAAQAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAkB0cmFrAAAAXHRraGQAAAADAAAAAAAAAAAAAAABAAAAAAAAA+gAAAAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAABAAAAAAEAAAABAAAAAAAAkZWR0cwAAABxlbHN0AAAAAAAAAAEAAAPoAAAAAAABAAAAAAG4bWRpYQAAACBtZGhkAAAAAAAAAAAAAAAAAABAAAAAQABVxAAAAAAALWhkbHIAAAAAAAAAAHZpZGUAAAAAAAAAAAAAAABWaWRlb0hhbmRsZXIAAAABY21pbmYAAAAUdm1oZAAAAAEAAAAAAAAAAAAAACRkaW5mAAAAHGRyZWYAAAAAAAAAAQAAAAx1cmwgAAAAAQAAASNzdGJsAAAAv3N0c2QAAAAAAAAAAQAAAK9hdmMxAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAEAAQABIAAAASAAAAAAAAAABFExhdmM2My4xLjEwMSBsaWJ4MjY0AAAAAAAAAAAAAAAAGP//AAAANWF2Y0MBZAAK/+EAGGdkAAqs2UQmwEQAAAMABAAAAwAIPEiWWAEABmjr48siwP34+AAAAAAQcGFzcAAAAAEAAAABAAAAFGJ0cnQAAAAAAAAWuAAAAAAAAAAYc3R0cwAAAAAAAAABAAAAAQAAQAAAAAAcc3RzYwAAAAAAAAABAAAAAQAAAAEAAAABAAAAFHN0c3oAAAAAAAAC1wAAAAEAAAAUc3RjbwAAAAAAAAABAAADRQAAAGF1ZHRhAAAAWW1ldGEAAAAAAAAAIWhkbHIAAAAAAAAAAG1kaXJhcHBsAAAAAAAAAAAAAAAALGlsc3QAAAAkqXRvbwAAABxkYXRhAAAAAQAAAABMYXZmNjMuMS4xMDEAAAAIZnJlZQAAAt9tZGF0AAACrQYF//+p3EXpvebZSLeWLNgg2SPu73gyNjQgLSBjb3JlIDE2NSByMzIyMiBiMzU2MDVhIC0gSC4yNjQvTVBFRy00IEFWQyBjb2RlYyAtIENvcHlsZWZ0IDIwMDMtMjAyNSAtIGh0dHA6Ly93d3cudmlkZW9sYW4ub3JnL3gyNjQuaHRtbCAtIG9wdGlvbnM6IGNhYmFjPTEgcmVmPTMgZGVibG9jaz0xOjA6MCBhbmFseXNlPTB4MzoweDExMyBtZT1oZXggc3VibWU9NyBwc3k9MSBwc3lfcmQ9MS4wMDowLjAwIG1peGVkX3JlZj0xIG1lX3JhbmdlPTE2IGNocm9tYV9tZT0xIHRyZWxsaXM9MSA4eDhkY3Q9MSBjcW09MCBkZWFkem9uZT0yMSwxMSBmYXN0X3Bza2lwPTEgY2hyb21hX3FwX29mZnNldD0tMiB0aHJlYWRzPTIgbG9va2FoZWFkX3RocmVhZHM9MSBzbGljZWRfdGhyZWFkcz0wIG5yPTAgZGVjaW1hdGU9MSBpbnRlcmxhY2VkPTAgYmx1cmF5X2NvbXBhdD0wIGNvbnN0cmFpbmVkX2ludHJhPTAgYmZyYW1lcz0zIGJfcHlyYW1pZD0yIGJfYWRhcHQ9MSBiX2JpYXM9MCBkaXJlY3Q9MSB3ZWlnaHRiPTEgb3Blbl9nb3A9MCB3ZWlnaHRwPTIga2V5aW50PTI1MCBrZXlpbnRfbWluPTEgc2NlbmVjdXQ9NDAgaW50cmFfcmVmcmVzaD0wIHJjX2xvb2thaGVhZD00MCByYz1jcmYgbWJ0cmVlPTEgY3JmPTIzLjAgcWNvbXA9MC42MCBxcG1pbj0wIHFwbWF4PTY5IHFwc3RlcD00IGlwX3JhdGlvPTEuNDAgYXE9MToxLjAwAIAAAAAiZYiEABX//vfJ78Cm69vetb+Tz0j4e8ZQD6wZq+vbSH/7MQ=="

}
