package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

/**
 * Content-addressable file store.
 *
 * Files are stored at:
 *   {appDir}/{id[0..1]}/{id[2..3]}/{id}
 *
 * The realPath is derived deterministically from [id], so path resolution
 * never needs a database query – but we persist it for diagnostics / tooling.
 *
 * [id]       SHA-256 hex digest of the full file content
 * [weakHash] SHA-256 hex digest of (first 4 KB ++ last 4 KB); used for the
 *            cheap first-pass duplicate check before computing the full hash.
 */
@Entity(
    tableName = "files",
    indices = [
        Index(value = ["size", "weak_hash"]),
    ]
)
data class DAppFile(
    @PrimaryKey val id: String,          // full SHA-256 hex (64 chars)
) : DEntityBase() {
    @ColumnInfo(name = "size")
    var size: Long = 0L

    @ColumnInfo(name = "mime_type")
    var mimeType: String = ""

    @ColumnInfo(name = "real_path")
    var realPath: String = ""

    @ColumnInfo(name = "ref_count")
    var refCount: Int = 1

    @ColumnInfo(name = "weak_hash")
    var weakHash: String = ""
}

@Dao
interface AppFileDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(file: DAppFile): Long

    @Query("SELECT * FROM files WHERE id = :id")
    fun getById(id: String): DAppFile?

    /** Cheap first-pass lookup – returns candidate rows before full-hash check. */
    @Query("SELECT * FROM files WHERE size = :size AND weak_hash = :weakHash")
    fun findByWeakKey(size: Long, weakHash: String): List<DAppFile>

    @Query("UPDATE files SET ref_count = ref_count + 1 WHERE id = :id")
    fun incrementRefCount(id: String)

    @Query("UPDATE files SET ref_count = ref_count - 1 WHERE id = :id")
    fun decrementRefCount(id: String)

    @Query("SELECT * FROM files WHERE ref_count <= 0")
    fun getOrphans(): List<DAppFile>

    @Query("DELETE FROM files WHERE id = :id")
    fun delete(id: String)

    @Update
    fun update(file: DAppFile)
}
