package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Update
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

/**
 * A share link. [id] doubles as the public `shared_id` that appears in the
 * link path. The `shared_token` is never stored – it is derived on demand via
 * HMAC(masterSecret, id), and [urlToken] is the dedicated key for guest `/fs` / `/zip/dir`.
 */
@Entity(tableName = "shares")
data class DShare(
    @PrimaryKey @ColumnInfo(name = "id") var id: String, // = shared_id
) : DEntityBase() {
    @ColumnInfo(name = "name")
    var name: String = ""

    /** Reserved for a future password feature; unused this release. Stored in plaintext. */
    @ColumnInfo(name = "password")
    var password: String = ""

    @ColumnInfo(name = "url_token")
    var urlToken: String = ""

    /** Valid until this instant; null = never expires. */
    @ColumnInfo(name = "expires_at")
    var expiresAt: Instant? = null

    @ColumnInfo(name = "read_only")
    var readOnly: Boolean = true

    /** Whitelisted roots of this share, stored as JSON. */
    @ColumnInfo(name = "data")
    var data: List<ShareRoot> = emptyList()

    val isExpired: Boolean
        get() = expiresAt?.let { it <= TimeHelper.now() } ?: false

    val isActive: Boolean
        get() = !isExpired
}

@Dao
interface ShareDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(share: DShare)

    @Query("SELECT * FROM shares WHERE id = :id")
    suspend fun getById(id: String): DShare?

    @Query("SELECT * FROM shares ORDER BY created_at DESC")
    suspend fun getAll(): List<DShare>

    @Query("DELETE FROM shares WHERE id = :id")
    suspend fun delete(id: String)

    @Update
    suspend fun update(share: DShare)
}