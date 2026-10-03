package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.platform.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TagsRustHttpTest {
    @Test
    fun allTypesUseRustWithAtomicMetadataAndTypedKeys() = runBlocking {
        val prefix = "tags-rust-${UUID.randomUUID()}"
        val key = "$prefix/文件, 'quoted'"
        val tags = mutableMapOf<DataType, String>()
        try {
            for (type in DataType.entries.filter { it != DataType.DEFAULT }) {
                val id = TagHelper.addOrUpdate("") { name = "$prefix-${type.name}"; this.type = type.value }
                tags[type] = id
                assertNull(AppDatabase.instance.tagDao().getById(id))
                TagHelper.addTagRelations(listOf(DTagRelation(tagId=id,key=key,type=type.value,title="original",size=5_000_000_001L)))
                val first = TagHelper.getTagRelationsByKey(key,type).single()
                assertEquals(5_000_000_001L,first.size)
                assertEquals("original",first.title)
                assertEquals(1,checkNotNull(TagHelper.get(id)).count)
                assertTrue(AppDatabase.instance.tagRelationDao().getAllByKey(key,type.value).isEmpty())
                TagHelper.editTagRelations(type,TagRelationStub(key,"updated",6_000_000_001L),listOf(id),emptyList())
                val second = TagHelper.getTagRelationsByKey(key,type).single()
                assertEquals(first.createdAt,second.createdAt)
                assertEquals("updated",second.title)
                assertEquals(6_000_000_001L,second.size)
                val originalTag = checkNotNull(TagHelper.get(id))
                TagHelper.addOrUpdate(id) { name = "$prefix-renamed" }
                assertEquals(originalTag.createdAt,checkNotNull(TagHelper.get(id)).createdAt)
            }
            TagHelper.deleteTagRelationByKeys(setOf(key),DataType.AUDIO)
            assertTrue(TagHelper.getTagRelationsByKey(key,DataType.AUDIO).isEmpty())
            assertEquals(1,TagHelper.getTagRelationsByKey(key,DataType.IMAGE).size)
            val imageId = tags.getValue(DataType.IMAGE)
            val another = TagHelper.addOrUpdate("") { name = "$prefix-extra"; type = DataType.IMAGE.value }
            tags[DataType.DEFAULT] = another
            TagHelper.addTagRelations(listOf(DTagRelation(tagId=another,key=key,type=DataType.IMAGE.value)))
            assertEquals(listOf(key),TagHelper.getKeysByTagIdsAsync(setOf(imageId,another)))
            var failed = false
            try {
                TagHelper.addTagRelations(listOf(
                    DTagRelation(tagId=imageId,key="$key/new",type=DataType.IMAGE.value),
                    DTagRelation(tagId=tags.getValue(DataType.VIDEO),key="$key/bad",type=DataType.IMAGE.value),
                ))
            } catch (_: Exception) { failed = true }
            assertTrue(failed)
            assertTrue(TagHelper.getTagRelationsByKey("$key/new",DataType.IMAGE).isEmpty())
            TagHelper.deleteTagRelationByKeysTagIds(setOf(key),setOf(imageId,another))
            assertTrue(TagHelper.getKeysByTagIdsAsync(setOf(imageId,another)).isEmpty())
            assertTrue(TagHelper.getKeysByTagIdsAsync(emptySet()).isEmpty())
        } finally {
            tags.values.forEach { TagHelper.delete(it) }
        }
    }
}
