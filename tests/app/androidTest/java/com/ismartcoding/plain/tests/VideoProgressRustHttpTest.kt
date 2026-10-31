package com.ismartcoding.plain.tests

import android.content.ContentValues
import android.provider.MediaStore
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.platform.media.deleteMedia
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class VideoProgressRustHttpTest {
    @Test
    fun videoPositionUsesRustAndRetainsLongMilliseconds() = runBlocking {
        val resolver = appContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "video-rust-${UUID.randomUUID()}.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/PlainSyntheticTests")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values))
        val id = checkNotNull(uri.lastPathSegment)
        try {
            checkNotNull(resolver.openOutputStream(uri)).use { it.write(Base64.decode(syntheticVideo, Base64.DEFAULT)) }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            assertEquals(1, resolver.update(uri, values, null, null))
            suspend fun current() = GraphqlApi.execute(Query.mediaPlayHistory("type:VIDEO ids:=$id", 0, 1)).single()
            GraphqlApi.execute(Mutation.saveMediaPlayProgress(id, DataType.VIDEO, 3_000_000_123L))
            val row = current()
            assertEquals(3_000_000_123L, row.positionMs)
            assertTrue(runCatching { GraphqlApi.execute(Mutation.saveMediaPlayProgress(id, DataType.VIDEO, -1)) }.isFailure)
            assertEquals(row, current())
            GraphqlApi.execute(Mutation.recordMediaPlay(id, DataType.VIDEO, 456))
            assertEquals(456L, current().positionMs)
            GraphqlApi.execute(Mutation.saveMediaPlayProgress(id, DataType.VIDEO, 0))
            assertEquals(0L, current().positionMs)
        } finally {
            deleteMedia(DataType.VIDEO, setOf(id), false)
        }
    }
}
