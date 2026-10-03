package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.DImageEmbedding
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.ImageEmbeddingHelper
import com.ismartcoding.plain.features.MediaDurationHelper
import com.ismartcoding.plain.platform.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MediaAuxRustHttpTest {
    @Test
    fun durationsAndEmbeddingsUseRustWithExactValuesAndRankedSearch() = runBlocking {
        val prefix = "media-aux-${UUID.randomUUID()}"
        val audioId = "$prefix-audio"
        val videoId = "$prefix-video"
        val bestId = "$prefix-best"
        val nextId = "$prefix-next"
        fun vector(first: Float): ByteArray = ByteBuffer.allocate(512 * 4).apply {
            putFloat(first)
            repeat(511) { putFloat(0f) }
        }.array()
        try {
            MediaDurationHelper.save("audio",audioId,5_000_000_001L)
            MediaDurationHelper.save("video",videoId,6_000_000_001L)
            TempData.mediaDurationMap.remove("audio:$audioId")
            MediaDurationHelper.restore()
            assertEquals(5_000_000_001L,TempData.mediaDurationMap["audio:$audioId"])
            assertEquals(6_000_000_001L,TempData.mediaDurationMap["video:$videoId"])
            assertTrue(AppDatabase.instance.mediaItemDao().getAll().none { it.mediaId.startsWith(prefix) })
            ImageEmbeddingHelper.insertAll(listOf(
                DImageEmbedding(bestId,"/synthetic/$bestId",vector(10f)),
                DImageEmbedding(nextId,"/synthetic/$nextId",vector(9f)),
            ))
            assertTrue(ImageEmbeddingHelper.getAllIds().containsAll(listOf(bestId,nextId)))
            assertTrue(AppDatabase.instance.imageEmbeddingDao().getAllIds().none { it.startsWith(prefix) })
            val results = ImageEmbeddingHelper.search(vector(1f),2)
            assertEquals(listOf(bestId,nextId),results.map { it.imageId })
            assertEquals(10f,results.first().score,0f)
            var failed = false
            try {
                ImageEmbeddingHelper.insertAll(listOf(
                    DImageEmbedding("$prefix-new","/synthetic/new",vector(1f)),
                    DImageEmbedding("$prefix-bad","/synthetic/bad",vector(Float.NaN)),
                ))
            } catch (_: Exception) { failed = true }
            assertTrue(failed)
            assertFalse(ImageEmbeddingHelper.getAllIds().contains("$prefix-new"))
            ImageEmbeddingHelper.deleteByIds(emptyList())
            assertTrue(ImageEmbeddingHelper.getAllIds().contains(bestId))
        } finally {
            ImageEmbeddingHelper.deleteByIds(listOf(bestId,nextId,"$prefix-new","$prefix-bad"))
            MediaDurationHelper.delete(DataType.AUDIO,listOf(audioId))
            MediaDurationHelper.delete(DataType.VIDEO,listOf(videoId))
        }
    }
}
