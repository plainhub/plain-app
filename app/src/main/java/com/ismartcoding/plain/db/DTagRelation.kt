package com.ismartcoding.plain.db

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.datetime.*
import com.ismartcoding.plain.helpers.TimeHelper

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
    fun getAllByKey(
        key: String,
        type: Int,
    ): List<DTagRelation>

    @Query("SELECT * FROM tag_relations WHERE `key` in (:keys) AND type=:type")
    fun getAllByKeys(
        keys: Set<String>,
        type: Int,
    ): List<DTagRelation>

    @Query("SELECT `key` FROM tag_relations WHERE tag_id=:tagId")
    fun getKeysByTagId(tagId: String): List<String>

    @Query("SELECT * FROM tag_relations WHERE tag_id in (:tagIds)")
    fun getAllByTagIds(tagIds: Set<String>): List<DTagRelation>

    @Query("DELETE FROM tag_relations WHERE `key` in (:keys) AND type=:type")
    fun deleteByKeys(keys: Set<String>, type: Int)

    @Query("DELETE FROM tag_relations WHERE type=:type")
    fun deleteByType(type: Int)

    @Query("DELETE FROM tag_relations WHERE tag_id=:tagId")
    fun deleteByTagId(tagId: String)

    @Query("DELETE FROM tag_relations WHERE `key` in (:keys) AND tag_id=:tagId")
    fun deleteByKeysTagId(keys: Set<String>, tagId: String)

    @Query("DELETE FROM tag_relations WHERE `key` in (:keys) AND tag_id in (:tagIds)")
    fun deleteByKeysTagIds(keys: Set<String>, tagIds: Set<String>)

    @RawQuery
    fun delete(query: SupportSQLiteQuery): Int

    @Insert
    fun insert(vararg item: DTagRelation)

    @Update
    fun update(vararg item: DTagRelation)

    @Query(
        "SELECT tags.id AS id, count(tag_relations.tag_id) AS count FROM tags JOIN tag_relations ON tags.id = tag_relations.tag_id WHERE tags.type=:type GROUP BY tags.id",
    )
    fun getAll(type: Int): List<DTagCount>
}

data class DTagCount(var id: String, var count: Int)
