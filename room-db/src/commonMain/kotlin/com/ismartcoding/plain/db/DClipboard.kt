package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

/**
 * A clipboard history entry. [source] is the client id of the origin: empty
 * means captured locally on this device, otherwise received from that peer.
 * [hash] is the SHA-256 of [text] and powers dedup and sync loop suppression.
 * [sensitive] mirrors the platform sensitive flag set by password managers.
 * [label] is an optional user label.
 */
@Entity(tableName = "clipboard", indices = [Index(value = ["hash"])])
data class DClipboard(
    @PrimaryKey @ColumnInfo(name = "id") var id: String,
    @ColumnInfo(name = "text") var text: String = "",
    @ColumnInfo(name = "hash") var hash: String = "",
    @ColumnInfo(name = "source") var source: String = "",
    @ColumnInfo(name = "label") var label: String = "",
    @ColumnInfo(name = "sensitive") var sensitive: Boolean = false,
    @ColumnInfo(name = "created_at") var createdAt: Instant = TimeHelper.now(),
)

@Dao
interface ClipboardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DClipboard)

    @Query("SELECT * FROM clipboard WHERE id = :id")
    suspend fun getById(id: String): DClipboard?

    @Query("SELECT * FROM clipboard ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatest(): DClipboard?

    @Query("SELECT * FROM clipboard WHERE hash = :hash ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestByHash(hash: String): DClipboard?

    /** [q] is the raw LIKE pattern with % wildcards; empty matches everything. */
    @Query(
        "SELECT * FROM clipboard WHERE text LIKE :q ESCAPE '\\' " +
            "OR label LIKE :q ESCAPE '\\' OR source LIKE :q ESCAPE '\\' " +
            "ORDER BY created_at DESC LIMIT :limit OFFSET :offset",
    )
    suspend fun getPage(limit: Int, offset: Int, q: String): List<DClipboard>

    @Query(
        "SELECT COUNT(*) FROM clipboard WHERE text LIKE :q ESCAPE '\\' " +
            "OR label LIKE :q ESCAPE '\\' OR source LIKE :q ESCAPE '\\'",
    )
    suspend fun count(q: String): Int

    @Query("DELETE FROM clipboard WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM clipboard")
    suspend fun clear()
}
