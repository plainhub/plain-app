package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.ismartcoding.lib.helpers.JsonHelper.jsonDecode
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
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
class DMessageFile(
    override var id: String = StringHelper.shortUUID(),
    val uri: String,
    val size: Long,
    val duration: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
) : IData

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
    @Transient val hasError: Boolean = false,
    val createdAt: Instant = Clock.System.now()
)

@Entity(tableName = "chats")
data class DChat(
    @PrimaryKey var id: String = StringHelper.shortUUID(),
) : DEntityBase() {
    @ColumnInfo(name = "is_me")
    var isMe: Boolean = false

    @ColumnInfo(name = "content")
    lateinit var content: DMessageContent

    val name: String
        get() {
            return if (isMe) {
                getString(R.string.me)
            } else {
                getString(R.string.app_name)
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
    val updatedAt: Instant = Clock.System.now(),
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats")
    fun getAll(): List<DChat>

    @Insert
    fun insert(vararg item: DChat)

    @Query("SELECT * FROM chats WHERE id=:id")
    fun getById(id: String): DChat?

    @Update
    fun update(vararg item: DChat)

    @Update(entity = DChat::class)
    fun updateData(item: ChatItemDataUpdate)

    @Query("DELETE FROM chats WHERE id = :id")
    fun delete(id: String)

    @Query("DELETE FROM chats WHERE id in (:ids)")
    fun deleteByIds(ids: List<String>)
}
