package com.ismartcoding.plain.api.graphql

import com.ismartcoding.plain.graphql.Fragments
import com.ismartcoding.plain.features.chat.models.*
import com.ismartcoding.plain.features.chat.ChatStatus
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.*
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Instant

class GraphqlModelDecodeTest {
    private val chat = """{"id":"chat-1","fromId":"me","toId":"peer-1","channelId":null,"content":{"__typename":"ChatMessageContent","text":"hello 中文","format":"PLAIN","items":[],"linkPreviews":[]},"status":"SENT","deliveryResults":[],"createdAt":"2026-10-12T00:00:00Z","updatedAt":"2026-10-12T01:00:00Z"}"""

    @Test
    fun chatDecodesTheWireEnvelopeWithoutChangingItsContent() {
        val value = JsonHelper.json.parseToJsonElement(chat)
        val item = JsonHelper.jsonDecodeFromElement<ChatItem>(value)
        assertNull(item.channelId)
        assertEquals(ChatStatus.SENT, item.status)
        assertEquals(Instant.parse("2026-10-12T01:00:00Z"), item.updatedAt)
        assertEquals("hello 中文", (item.messageContent.value as ChatMessage).text)
        assertEquals(value, JsonHelper.jsonEncodeToElement(item))
    }

    @Test
    fun unifiedMessageProjectsFromTheMainGraphqlResult() {
        val fixture = chat.replace(
            """{"__typename":"ChatMessageContent","text":"hello 中文","format":"PLAIN","items":[],"linkPreviews":[]}""",
            """{"__typename":"ChatMessageContent","text":"hello 中文","format":"MARKDOWN","items":[{"type":"IMAGE","attachment":{"id":"a","uri":"fid:image","size":3}},{"type":"FILE","attachment":{"id":"b","uri":"fsid:file","size":5}}],"linkPreviews":[]}""",
        )
        val content = JsonHelper.jsonDecode<ChatItem>(fixture).messageContent
        assertEquals("MESSAGE", content.type.name)
        val message = content.value as ChatMessage
        assertEquals(ChatTextFormat.MARKDOWN, message.format)
        assertEquals("hello 中文", message.text)
        assertEquals(listOf("fid:image"), message.images.map { it.uri })
        assertEquals(listOf("fsid:file"), message.files.map { it.uri })
        val request = com.ismartcoding.plain.graphql.Mutation.sendChatItems(listOf("peer:local"), ChatContentInput.from(content))
        val sent = request.operation.variables.getValue("content").jsonObject
        assertEquals(setOf("message"), sent.keys)
        val payload = sent.getValue("message").jsonObject
        assertEquals(setOf("text", "format", "items"), payload.keys)
        assertEquals("MARKDOWN", payload.getValue("format").jsonPrimitive.content)
        assertEquals(listOf("IMAGE", "FILE"), payload.getValue("items").jsonArray.map { it.jsonObject.getValue("type").jsonPrimitive.content })
        assertTrue(request.operation.query.contains("... on ChatMessageContent"))
        assertFalse(request.operation.query.contains("... on ChatTextContent"))
    }

    @Test
    fun plainMixedMessageKeepsDerivedLinkPreviewMetadataWithoutSendingItBack() {
        assertTrue(Fragments.CHAT_ITEM.contains("description image imageWidth"))
        assertFalse(Fragments.CHAT_ITEM.contains("imageLocalPath"))
        assertFalse(Fragments.CHAT_ITEM.contains("imageUrl"))
        val url = "https://example.org/article"
        val fixture = chat.replace(
            """{"__typename":"ChatMessageContent","text":"hello 中文","format":"PLAIN","items":[],"linkPreviews":[]}""",
            """{"__typename":"ChatMessageContent","text":"$url","format":"PLAIN","items":[{"type":"IMAGE","attachment":{"id":"a","uri":"fid:image","size":3}},{"type":"FILE","attachment":{"id":"b","uri":"fid:file","size":5}}],"linkPreviews":[{"url":"$url","title":"Article","description":"Preview summary","image":"fid:cover","imageWidth":32,"imageHeight":24,"siteName":"Example Site","domain":"example.org","createdAt":"2026-10-12T00:00:00Z"}]}""",
        )
        val content = JsonHelper.jsonDecode<ChatItem>(fixture).messageContent
        val message = content.value as ChatMessage
        assertEquals(ChatTextFormat.PLAIN, message.format)
        assertEquals(url, message.text)
        assertEquals(2, message.items.size)
        val preview = message.linkPreviews.single()
        assertEquals("Article", preview.title)
        assertEquals("Preview summary", preview.description)
        assertEquals("fid:cover", preview.imageReference)
        for (invalid in listOf(null, "fid:", "https://example.org/cover.png", "/cache/cover.png")) {
            assertNull(preview.copy(image = invalid).imageReference)
        }
        assertEquals("Example Site", preview.siteName)
        assertEquals(32, preview.imageWidth)
        assertEquals(24, preview.imageHeight)
        val input = JsonHelper.jsonEncodeToElement(ChatContentInput.from(content)).jsonObject
        assertFalse("linkPreviews" in input.getValue("message").jsonObject)
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
        val feed = JsonHelper.jsonDecode<Feed>("""{"id":"feed-1","name":"Feed","url":"https://example.org/feed","logo":"/feeds/icon.png","fetchContent":true,"lastSyncAt":null,"lastError":{"code":"NONE","detail":""},"createdAt":"2026-10-12T00:00:00Z","updatedAt":"2026-10-12T00:00:00Z"}""")
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
            MessageContent(MessageType.MESSAGE, ChatMessage(text = "hello")),
            MessageContent(MessageType.MESSAGE, ChatMessage(items = listOf(
                ChatMessageAttachment(ChatAttachmentType.IMAGE, MessageFile(uri = "fid:image", size = 3)),
                ChatMessageAttachment(ChatAttachmentType.FILE, MessageFile(uri = "fsid:file", size = 5, durationMs = 30000)),
            ))),
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
        val requests = listOf(Query.peers())
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

    @Test
    fun closedPhoneAndFeedValuesRoundTripAndRejectUnknownCodes() {
        for (kind in PhoneNumberType.entries) {
            val wire = "\"${kind.name}\""
            assertEquals(kind, JsonHelper.jsonDecode<PhoneNumberType>(wire))
            assertEquals(wire, JsonHelper.jsonEncode(kind))
        }
        for (code in FeedErrorCode.entries) {
            val wire = """{"code":"${code.name}","detail":"fixture"}"""
            val error = JsonHelper.jsonDecode<FeedError>(wire)
            assertEquals(code, error.code)
            assertEquals(wire, JsonHelper.jsonEncode(error))
            assertEquals(code != FeedErrorCode.NONE, Feed(lastError = error).hasSyncError)
        }
        for (invalid in listOf("", "INVALID", "mobile")) {
            assertFailsWith<SerializationException> {
                JsonHelper.jsonDecode<PhoneNumberType>("\"$invalid\"")
            }
            assertFailsWith<SerializationException> {
                JsonHelper.jsonDecode<FeedError>("""{"code":"$invalid","detail":"fixture"}""")
            }
        }
    }

}
