package com.ismartcoding.plain.db

import androidx.room.*
import com.ismartcoding.lib.helpers.StringHelper
import kotlinx.datetime.*
import java.util.ArrayList

@Entity(tableName = "vocabularies")
data class DVocabulary(
    @PrimaryKey var id: String = StringHelper.shortUUID()
) : DEntityBase() {
    @ColumnInfo(name = "box_id")
    var boxId: String = ""

    @ColumnInfo(name = "words")
    var words: ArrayList<String> = arrayListOf()

    @ColumnInfo(name = "name")
    var name: String = ""
}

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabularies WHERE box_id=:boxId")
    fun getAll(boxId: String): List<DVocabulary>

    @Query("SELECT * FROM vocabularies WHERE id=:id")
    fun getById(id: String): DVocabulary?

    @Insert
    fun insert(vararg item: DVocabulary)

    @Update
    fun update(vararg item: DVocabulary)

    @Delete
    fun delete(item: DVocabulary)
}

