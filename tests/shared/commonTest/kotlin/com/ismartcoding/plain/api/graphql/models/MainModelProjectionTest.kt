package com.ismartcoding.plain.models

import androidx.compose.ui.unit.IntSize
import com.ismartcoding.plain.IData
import com.ismartcoding.plain.ui.extensions.getRotatedSize
import com.ismartcoding.plain.lib.JsonHelper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.jsonObject

class MainModelProjectionTest {
    @Test
    fun mediaModelsDecodeServerMetadataAndServeTheItemInterfaceDirectly() {
        val image = JsonHelper.jsonDecode<Image>("""{"id":"image","title":"Photo","path":"/photo","size":42,"bucketId":"camera","createdAt":"2026-10-15T00:00:00Z","updatedAt":"2026-10-15T00:00:00Z","takenAt":null,"isFavorite":true,"width":640,"height":480,"rotation":90}""")
        val item: IData = image
        assertEquals("image", item.id)
        assertEquals(IntSize(480, 640), image.getRotatedSize())
        val doc = JsonHelper.jsonDecode<Doc>("""{"id":"document","title":"Report","path":"/report","size":99,"bucketId":"documents","createdAt":"2026-10-15T00:00:00Z","updatedAt":"2026-10-15T00:00:00Z","extension":"pdf"}""")
        assertEquals("pdf", doc.extension)
        assertEquals(99L, doc.size)
    }

    @Test
    fun notificationAndBucketUseTheMainSchemaFieldNames() {
        val notification = JsonHelper.jsonDecode<Notification>("""{"id":"notification","onlyOnce":false,"isClearable":true,"appId":"fixture","appName":"Fixture","postedAt":"2026-10-15T00:00:00Z","silent":false,"title":"Title","body":"Body","actions":[],"replyActions":[]}""")
        val encoded = JsonHelper.jsonEncodeToElement(notification).jsonObject
        assertEquals("2026-10-15T00:00:00Z", notification.postedAt.toString())
        assertEquals(false, encoded.containsKey("time"))
        val bucket = JsonHelper.jsonDecode<MediaBucket>("""{"id":"camera","name":"Camera","itemCount":2,"size":42,"topItemPaths":["/photo"]}""")
        assertEquals(listOf("/photo"), bucket.topItemPaths)
        assertEquals(42L, bucket.size)
    }
}
