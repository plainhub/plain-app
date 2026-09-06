package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query

@Entity(tableName = "image_editor_projects")
data class DImageEditorProject(
    @PrimaryKey val id: String,
) : DEntityBase() {
    @ColumnInfo(name = "state_b64")
    var stateB64: String = ""

    @ColumnInfo(name = "thumbnail")
    var thumbnail: String? = null

    @ColumnInfo(name = "canvas_width")
    var canvasWidth: Int = 0

    @ColumnInfo(name = "canvas_height")
    var canvasHeight: Int = 0

    @ColumnInfo(name = "layer_count")
    var layerCount: Int = 0
}

@Dao
interface ImageEditorProjectDao {
    @Query("SELECT * FROM image_editor_projects ORDER BY updated_at DESC LIMIT :limit")
    suspend fun list(limit: Int): List<DImageEditorProject>

    @Query("SELECT * FROM image_editor_projects WHERE id = :id")
    suspend fun getById(id: String): DImageEditorProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: DImageEditorProject)

    @Query("DELETE FROM image_editor_projects WHERE id = :id")
    suspend fun delete(id: String)
}
