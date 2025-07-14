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
import com.ismartcoding.plain.data.IData
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
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

class DMessageContent(val type: String, var value: Any? = null)

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
    fun isRemoteFile(): Boolean {
        return uri.startsWith("fid:")
    }

    fun parseFileId(): String {
        return uri.replace("fid:", "")
    }

    fun getPreviewPath(context: Context, peer: DPeer?): String {
        return if (uri.startsWith("fid:")) {
            peer?.getFileUrl(parseFileId()) + "&w=200&h=200"
        } else {
            uri.getFinalPath(context)
        }
    }
}

@Serializable
class DMessageImages(val items: List<DMessageFile>)

@Serializable
class DMessageFiles(val items: List<DMessageFile>)

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
    val createdAt: Instant = Clock.System.now()
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

    @ColumnInfo(name = "group_id", index = true)
    var groupId: String = "" // chat group id, empty if not a group chat

    @ColumnInfo(name = "status")
    var status: String = "" // pending, sent, failed

    @ColumnInfo(name = "content")
    lateinit var content: DMessageContent

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
    val updatedAt: Instant = Clock.System.now(),
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats")
    fun getAll(): List<DChat>

    @Query("SELECT * FROM chats WHERE to_id = :toId OR from_id = :toId ORDER BY created_at ASC")
    fun getByChatId(toId: String): List<DChat>

    @Query(
        """
        SELECT * FROM chats c
        INNER JOIN (
            SELECT from_id, to_id, MAX(created_at) as max_created_at
            FROM chats 
            GROUP BY from_id, to_id
        ) latest ON c.from_id = latest.from_id 
                 AND c.to_id = latest.to_id 
                 AND c.created_at = latest.max_created_at
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

    @Update(entity = DChat::class)
    fun updateData(item: ChatItemDataUpdate)

    @Query("DELETE FROM chats WHERE id = :id")
    fun delete(id: String)

    @Query("DELETE FROM chats WHERE id in (:ids)")
    fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM chats WHERE to_id = :peerId OR from_id = :peerId")
    fun deleteByPeerId(peerId: String)
}
