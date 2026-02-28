package com.ismartcoding.plain.db

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.helpers.JsonHelper.jsonDecode
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.helpers.FileHelper
import kotlin.time.Instant
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.serialization.Serializable
import org.json.JSONObject

fun DMessageContent.toJSONString(): String {
    val obj = JSONObject()
    obj.put("type", type)
    if (value != null) {
        var valueJSON = "{}"
        when (type) {
            DMessageType.TEXT.value -> {
                valueJSON = jsonEncode(value as DMessageText)
            }

            DMessageType.IMAGES.value -> {
                valueJSON = jsonEncode(value as DMessageImages)
            }

            DMessageType.FILES.value -> {
                valueJSON = jsonEncode(value as DMessageFiles)
            }
        }
        obj.put("value", JSONObject(valueJSON))
    }
    return obj.toString()
}

class DMessageContent(val type: String, var value: Any? = null) {
    /**
     * Rewrite local file/image URIs to `fsid:` references for transmission.
     */
    fun toPeerMessageContent(): DMessageContent {
        return when (type) {
            DMessageType.FILES.value -> {
                val files = value as DMessageFiles
                val modified = files.items.map { file ->
                    val fileId = FileHelper.getFileId(file.uri)
                    file.copy(uri = "fsid:$fileId")
                }
                DMessageContent(type, DMessageFiles(modified))
            }

            DMessageType.IMAGES.value -> {
                val images = value as DMessageImages
                val modified = images.items.map { image ->
                    val fileId = FileHelper.getFileId(image.uri)
                    image.copy(uri = "fsid:$fileId")
                }
                DMessageContent(type, DMessageImages(modified))
            }

            else -> this
        }
    }
}

enum class DMessageType(val value: String) {
    TEXT("text"),
    IMAGES("images"),
    FILES("files"),
}

@Serializable
class DMessageText(val text: String, val linkPreviews: List<DLinkPreview> = emptyList())

@Serializable
data class DMessageFile(
    override var id: String = StringHelper.shortUUID(),
    val uri: String,
    val size: Long,
    val duration: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val summary: String = "",
    val fileName: String = "",
) : IData {
    /** True when this file must be downloaded from a remote peer (fsid: scheme). */
    fun isRemoteFile(): Boolean {
        return uri.startsWith("fsid:")
    }

    /**
     * True when this file is stored in the local content-addressable store.
     * The [uri] has the form  fid:{sha256hex}.
     */
    fun isFidFile(): Boolean {
        return uri.startsWith("fid:")
    }

    /**
     * Returns the SHA-256 fileId for fid: URIs.
     * Returns an empty string for other URI schemes.
     */
    fun localFileId(): String {
        return if (isFidFile()) uri.removePrefix("fid:") else ""
    }

    /** Remote fileId extracted from a fsid: URI (used as query param for /fs endpoint). */
    fun parseFileId(): String {
        return uri.replace("fsid:", "")
    }

    fun getPreviewPath(context: Context, peer: DPeer?): String {
        return if (isRemoteFile()) {
            peer?.getFileUrl(parseFileId()) + "&w=200&h=200"
        } else {
            // Handles fid:, app://, and absolute paths via getFinalPath extension
            uri.getFinalPath(context)
        }
    }
}

@Serializable
class DMessageImages(val items: List<DMessageFile>)

@Serializable
class DMessageFiles(val items: List<DMessageFile>)

/**
 * Per-member delivery result for a single recipient.
 * [error] is null when the message was delivered successfully.
 */
@Serializable
data class DMessageDeliveryResult(
    val peerId: String,
    val peerName: String,
    val error: String? = null,
)

/**
 * Aggregated delivery status for a channel broadcast.
 * Stored as JSON in the [DChat.statusData] column.
 */
@Serializable
data class DMessageStatusData(
    val results: List<DMessageDeliveryResult> = emptyList(),
) {
    val total: Int get() = results.size
    val deliveredCount: Int get() = results.count { it.error == null }
    val failedCount: Int get() = results.count { it.error != null }
    val failedResults: List<DMessageDeliveryResult> get() = results.filter { it.error != null }
    val deliveredResults: List<DMessageDeliveryResult> get() = results.filter { it.error == null }
    val allDelivered: Boolean get() = total > 0 && failedCount == 0
    val allFailed: Boolean get() = total > 0 && deliveredCount == 0
    val hasPartialFailure: Boolean get() = deliveredCount > 0 && failedCount > 0
    fun deliveryLabel(): String = "$deliveredCount/$total"

    companion object {
        fun fromJson(json: String): DMessageStatusData? {
            if (json.isEmpty()) return null
            return try {
                jsonDecode<DMessageStatusData>(json)
            } catch (e: Exception) {
                null
            }
        }
    }
}

@Serializable
class DLinkPreview(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val imageLocalPath: String? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val siteName: String? = null,
    val domain: String? = null,
    @kotlinx.serialization.Transient val hasError: Boolean = false,
    val createdAt: Instant = TimeHelper.now()
)

@Entity(
    tableName = "chats",
)
data class DChat(
    @PrimaryKey var id: String = StringHelper.shortUUID(),
) : DEntityBase() {
    @ColumnInfo(name = "from_id", index = true)
    var fromId: String = "" // me|local|peer_id

    @ColumnInfo(name = "to_id", index = true)
    var toId: String = "" // me|local|peer_id

    @ColumnInfo(name = "channel_id", index = true)
    var channelId: String = "" // chat channel id, empty if not a channel chat

    @ColumnInfo(name = "status")
    var status: String = "" // pending, sent, partial, failed

    /**
     * JSON-encoded [DMessageStatusData], populated for channel broadcast messages.
     * Empty string means no per-member data is available.
     */
    @ColumnInfo(name = "status_data", defaultValue = "")
    var statusData: String = ""

    @ColumnInfo(name = "content")
    lateinit var content: DMessageContent

    fun parseStatusData(): DMessageStatusData? = DMessageStatusData.fromJson(statusData)

    fun getMessagePreview(): String {
        return when (content.type) {
            DMessageType.TEXT.value -> {
                val textMessage = content.value as? DMessageText
                textMessage?.text?.take(50) ?: getString(R.string.message)
            }

            DMessageType.IMAGES.value -> {
                val imagesMessage = content.value as? DMessageImages
                val items = imagesMessage?.items ?: emptyList()
                val videoCount = items.count { it.duration > 0 }
                val imageCount = items.size - videoCount
                when {
                    imageCount > 0 && videoCount > 0 -> {
                        val imgPart = if (imageCount > 1) "$imageCount ${getString(R.string.images)}" else getString(R.string.image)
                        val vidPart = if (videoCount > 1) "$videoCount ${getString(R.string.videos)}" else getString(R.string.video)
                        "$imgPart, $vidPart"
                    }

                    videoCount > 0 -> {
                        if (videoCount > 1) "$videoCount ${getString(R.string.videos)}" else getString(R.string.video)
                    }

                    else -> {
                        if (imageCount > 1) "$imageCount ${getString(R.string.images)}" else getString(R.string.image)
                    }
                }
            }

            DMessageType.FILES.value -> {
                val filesMessage = content.value as? DMessageFiles
                val count = filesMessage?.items?.size ?: 0
                if (count > 1) {
                    "$count ${getString(R.string.files)}"
                } else {
                    getString(R.string.file)
                }
            }

            else -> getString(R.string.message)
        }
    }

    companion object {
        fun parseContent(content: String): DMessageContent {
            val obj = JSONObject(content)
            val message = DMessageContent(obj.optString("type"))
            val valueJson = obj.optString("value")
            when (message.type) {
                DMessageType.TEXT.value -> {
                    message.value = jsonDecode<DMessageText>(valueJson)
                }

                DMessageType.IMAGES.value -> {
                    message.value = jsonDecode<DMessageImages>(valueJson)
                }

                DMessageType.FILES.value -> {
                    message.value = jsonDecode<DMessageFiles>(valueJson)
                }
            }

            return message
        }
    }
}

data class ChatItemDataUpdate(
    var id: String,
    var content: DMessageContent,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = TimeHelper.now(),
)

data class ChatItemStatusUpdate(
    val id: String,
    val status: String,
    @ColumnInfo(name = "status_data")
    val statusData: String = "",
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = TimeHelper.now(),
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats")
    fun getAll(): List<DChat>

    @Query("SELECT * FROM chats WHERE channel_id = '' AND (to_id = :toId OR from_id = :toId) ORDER BY created_at ASC")
    fun getByChatId(toId: String): List<DChat>

    @Query("SELECT * FROM chats WHERE channel_id = :channelId ORDER BY created_at ASC")
    fun getByChannelId(channelId: String): List<DChat>

    @Query(
        """
        SELECT c.* FROM chats c
        INNER JOIN (
            SELECT '' as from_id, '' as to_id, channel_id, MAX(created_at) as max_created_at
            FROM chats
            WHERE channel_id != ''
            GROUP BY channel_id
            UNION ALL
            SELECT from_id, to_id, '' as channel_id, MAX(created_at) as max_created_at
            FROM chats
            WHERE channel_id = ''
            GROUP BY from_id, to_id
        ) latest ON (
            (c.channel_id != '' AND c.channel_id = latest.channel_id AND c.created_at = latest.max_created_at)
            OR
            (c.channel_id = '' AND c.from_id = latest.from_id AND c.to_id = latest.to_id AND c.created_at = latest.max_created_at)
        )
        ORDER BY c.created_at DESC
    """
    )
    fun getAllLatestChats(): List<DChat>

    @Insert
    fun insert(vararg item: DChat)

    @Query("SELECT * FROM chats WHERE id=:id")
    fun getById(id: String): DChat?

    @Update
    fun update(vararg item: DChat)

    @Query("UPDATE chats SET status = :status WHERE id = :id")
    fun updateStatus(id: String, status: String)

    @Query("UPDATE chats SET status = :status, status_data = :statusData WHERE id = :id")
    fun updateStatusAndData(id: String, status: String, statusData: String)

    @Update(entity = DChat::class)
    fun updateStatusData(item: ChatItemStatusUpdate)

    @Update(entity = DChat::class)
    fun updateData(item: ChatItemDataUpdate)

    @Query("DELETE FROM chats WHERE id = :id")
    fun delete(id: String)

    @Query("DELETE FROM chats WHERE id in (:ids)")
    fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM chats WHERE channel_id = '' AND (to_id = :peerId OR from_id = :peerId)")
    fun deleteByChatId(peerId: String)

    @Query("DELETE FROM chats WHERE channel_id = :channelId")
    fun deleteByChannelId(channelId: String)
}
