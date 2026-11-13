package com.ismartcoding.plain.api.graphql

import com.ismartcoding.plain.features.chat.models.*
import com.ismartcoding.plain.features.chat.ChatStatus
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.*
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Instant

class GraphqlModelDecodeTest {
    private val chat = """{"id":"chat-1","fromId":"me","toId":"peer-1","channelId":null,"content":"{\"type\":\"TEXT\",\"value\":{\"text\":\"hello 中文\"}}","status":"SENT","statusData":"","createdAt":"2026-10-12T00:00:00Z","updatedAt":"2026-10-12T01:00:00Z"}"""

    @Test
    fun chatDecodesTheWireEnvelopeWithoutChangingItsContent() {
        val value = JsonHelper.json.parseToJsonElement(chat)
        val item = JsonHelper.jsonDecodeFromElement<ChatItem>(value)
        assertNull(item.channelId)
        assertEquals(ChatStatus.SENT, item.status)
        assertEquals(Instant.parse("2026-10-12T01:00:00Z"), item.updatedAt)
        assertEquals("hello 中文", (item.messageContent.value as MessageText).text)
        assertEquals(value, JsonHelper.jsonEncodeToElement(item))
    }

    @Test
    fun selectedRequiredFieldsCannotSilentlyBecomeDraftDefaults() {
        assertFailsWith<SerializationException> {
            JsonHelper.jsonDecode<ChatItem>(chat.replace("\"status\":\"SENT\",", ""))
        }
    }

    @Test
    fun nestedConversationAndEditResultsDecodeAsModels() {
        val conversation = JsonHelper.jsonDecode<ChatConversation>("""{"id":"peer-1","chat":$chat}""")
        val edited = JsonHelper.jsonDecode<ChatTextEdit>("""{"changed":true,"chat":$chat}""")
        assertEquals(conversation.chat, edited.chat)
        assertEquals("peer-1", conversation.id)
    }

    @Test
    fun selectedFeedFieldsDecodeWithoutResolverBackedRelationships() {
        val feed = JsonHelper.jsonDecode<Feed>("""{"id":"feed-1","name":"Feed","url":"https://example.org/feed","logo":"/feeds/icon.png","fetchContent":true,"lastSyncAt":null,"lastError":{"code":"","detail":""},"createdAt":"2026-10-12T00:00:00Z","updatedAt":"2026-10-12T00:00:00Z"}""")
        assertEquals("/feeds/icon.png", feed.logo)
        assertNull(feed.lastSyncAt)
        assertEquals(false, feed.hasSyncError)
        assertEquals(0, feed.entryCount)
    }

    @Test
    fun audioDecodesWithoutLoadingTags() {
        val audio = JsonHelper.jsonDecode<Audio>("""{"id":"audio-1","title":"Track","artist":"Artist","path":"/track.mp3","durationMs":1000,"size":2000,"bucketId":"bucket-1","albumFileId":"","createdAt":"2026-10-12T00:00:00Z","updatedAt":"2026-10-12T00:00:00Z","isFavorite":false}""")
        assertEquals("audio-1", audio.id)
        assertEquals(false, JsonHelper.jsonEncodeToElement(audio).toString().contains("\"tags\""))
    }

    @Test
    fun playlistItemsDecodeMediaIdsAndPaths() {
        val item = JsonHelper.jsonDecode<AudioItem>("""{"mediaId":"audio-1","path":"/track.mp3","title":"Track","artist":"Artist","durationMs":1000}""")
        assertEquals("audio-1", item.mediaId)
        assertEquals("/track.mp3", item.path)
        assertEquals(1000L, item.durationMs)
    }

    @Test
    fun allMediaModelsImplementTheSharedFields() {
        listOf(Audio::class.java, Image::class.java, Video::class.java, Doc::class.java).forEach { type ->
            assertEquals(true, MediaItem::class.java.isAssignableFrom(type), type.simpleName)
        }
    }

    @Test
    fun hostModelsPreserveLongScalarsAndSchemaFieldNames() {
        val relation = JsonHelper.jsonDecode<TagRelation>("""{"tagId":"tag-1","key":"file-1"}""")
        assertEquals("file-1", relation.key)
    }

    @Test
    fun messageContentUsesTheSharedJsonHelperForEveryEnvelopeType() {
        val messages = listOf(
            MessageContent(MessageType.TEXT, MessageText("hello")),
            MessageContent(MessageType.IMAGES, MessageImages(listOf(MessageFile(uri = "fid:image", size = 3)))),
            MessageContent(MessageType.FILES, MessageFiles(listOf(MessageFile(uri = "fsid:file", size = 5, durationMs = 30000)))),
            MessageContent(MessageType.SHARE, MessageShare("share-1", "secret", SharePeerInfo("peer", "127.0.0.1", 8080), "Folder")),
        )
        messages.forEach { message ->
            val encoded = JsonHelper.jsonEncode(message)
            val decoded = JsonHelper.jsonDecode<MessageContent>(encoded)
            assertEquals(message.type, decoded.type)
            assertEquals(JsonHelper.json.parseToJsonElement(encoded), JsonHelper.jsonEncodeToElement(decoded))
        }
    }

    @Test
    fun mediaInfoUnionUsesTheGraphqlTypename() {
        val info = JsonHelper.jsonDecode<MediaFileInfo>("""{"__typename":"VideoFileInfo","width":1920,"height":1080,"durationMs":30000,"location":null}""") as VideoFileInfo
        assertEquals(30000L, info.durationMs)
    }
    @Test
    fun peerFieldsUseMainCamelCaseFieldsWithoutDerivedAddress() {
        val peer = JsonHelper.jsonDecode<Peer>("""{"id":"peer-1","name":"Peer","ip":"127.0.0.1","key":"key","publicKey":"public","status":"PAIRED","port":8080,"deviceType":"PHONE","token":"","managed":true,"createdAt":"2026-10-12T00:00:00Z","updatedAt":"2026-10-12T00:00:00Z"}""")
        assertEquals("public", peer.publicKey)
        assertEquals("127.0.0.1", peer.ip)
        assertTrue(peer.managed)
        val encoded = JsonHelper.jsonEncodeToElement(peer).jsonObject
        for (field in listOf("publicKey", "deviceType", "createdAt", "updatedAt", "managed")) assertTrue(field in encoded)
        for (field in listOf("public_key", "device_type", "created_at", "updated_at", "address")) assertFalse(field in encoded)
        assertEquals(peer, JsonHelper.jsonDecodeFromElement<Peer>(JsonHelper.jsonEncodeToElement(peer)))
        assertFalse(JsonHelper.jsonDecodeFromElement<Peer>(JsonHelper.jsonEncodeToElement(peer.copy(managed = false))).managed)
    }

    @Test
    fun peerQueriesIncludeManagementStatus() {
        val requests = listOf(Query.peers(), Query.chatDownloadQueue(), Query.chatDownloadQueue(includePeerCredentials = false))
        requests.forEach { request ->
            assertTrue(Regex("\\bmanaged\\b").containsMatchIn(request.operation.query), request.operation.query)
        }
    }

    @Test
    fun channelUsesTheMainGraphqlObjectAndMemberArray() {
        val channel = JsonHelper.jsonDecode<ChatChannel>("""{"id":"channel-1","name":"Channel","ownerId":"owner-1","actions":{"isOwner":false,"canRename":false,"canManageMembers":false,"canLeave":true,"canDelete":false},"members":[{"peerId":"peer-1","status":"JOINED"}],"version":4294967296,"status":"JOINED","createdAt":"2026-10-12T00:00:00Z","updatedAt":"2026-10-12T00:00:00Z"}""")
        assertEquals(4294967296L, channel.version)
        assertEquals("owner-1", channel.ownerId)
        assertEquals(listOf("peer-1"), channel.members.map { it.peerId })
        assertTrue(channel.actions.canLeave)
        assertFalse(channel.actions.canDelete)
        assertEquals(channel, JsonHelper.jsonDecodeFromElement<ChatChannel>(JsonHelper.jsonEncodeToElement(channel)))
    }

    @Test
    fun nativeRecordDecodersKeepRejectingIncompleteSnapshots() {
        assertFailsWith<SerializationException> { JsonHelper.jsonDecode<Peer>("""{"id":"peer-1"}""") }
        assertFailsWith<SerializationException> { JsonHelper.jsonDecode<ChatChannel>("""{"id":"channel-1"}""") }
    }

}
