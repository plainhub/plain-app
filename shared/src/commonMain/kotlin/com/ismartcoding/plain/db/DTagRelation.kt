package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Update
import com.ismartcoding.plain.helpers.TimeHelper
import kotlin.time.Instant

@Entity(tableName = "tag_relations", primaryKeys = ["tag_id", "key", "type"])
data class DTagRelation(
    @ColumnInfo(name = "tag_id")
    var tagId: String = "",
    var key: String = "",
    var type: Int = 0,
) {
    @ColumnInfo(name = "created_at")
    var createdAt: Instant = TimeHelper.now()
    var size: Long = 0
    var title: String = ""
}

@Dao
interface TagRelationDao {
    @Query("SELECT * FROM tag_relations WHERE `key`=:key AND type=:type")
    suspend fun getAllByKey(key: String, type: Int): List<DTagRelation>

    @Query("SELECT * FROM tag_relations WHERE `key` in (:keys) AND type=:type")
    suspend fun getAllByKeys(keys: Set<String>, type: Int): List<DTagRelation>

    @Query("SELECT `key` FROM tag_relations WHERE tag_id=:tagId")
    suspend fun getKeysByTagId(tagId: String): List<String>

    @Query("SELECT * FROM tag_relations WHERE tag_id in (:tagIds)")
    suspend fun getAllByTagIds(tagIds: Set<String>): List<DTagRelation>

    @Query("DELETE FROM tag_relations WHERE `key` in (:keys) AND type=:type")
    suspend fun deleteByKeys(keys: Set<String>, type: Int)

    @Query("DELETE FROM tag_relations WHERE type=:type")
    suspend fun deleteByType(type: Int)

    @Query("DELETE FROM tag_relations WHERE tag_id=:tagId")
    suspend fun deleteByTagId(tagId: String)

    @Query("DELETE FROM tag_relations WHERE `key` in (:keys) AND tag_id=:tagId")
    suspend fun deleteByKeysTagId(keys: Set<String>, tagId: String)

    @Query("DELETE FROM tag_relations WHERE `key` in (:keys) AND tag_id in (:tagIds)")
    suspend fun deleteByKeysTagIds(keys: Set<String>, tagIds: Set<String>)

    @RawQuery
    suspend fun delete(query: RoomRawQuery): Int

    @Insert
    suspend fun insert(vararg item: DTagRelation)

    @Update
    suspend fun update(vararg item: DTagRelation)

    @Query(
        "SELECT tags.id AS id, count(tag_relations.tag_id) AS count FROM tags JOIN tag_relations ON tags.id = tag_relations.tag_id WHERE tags.type=:type GROUP BY tags.id",
    )
    suspend fun getAll(type: Int): List<DTagCount>
}

data class DTagCount(var id: String, var count: Int)
