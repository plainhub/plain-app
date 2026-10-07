package com.ismartcoding.plain.channel

import com.ismartcoding.plain.chat.channel.channelFacts
import com.ismartcoding.plain.db.ChannelMember
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.enums.ChannelMemberStatus
import com.ismartcoding.plain.enums.ChatChannelStatus
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ChannelFactsTest {
    @Test
    fun publicChannelPayloadPreservesMembersAndOmitsEncryptionKey() {
        val channel = DChatChannel(
            id = "channel-1",
            name = "Team",
            key = "private-channel-key",
            ownerId = "owner-1",
            members = listOf(
                ChannelMember("owner-1", ChannelMemberStatus.JOINED),
                ChannelMember("peer-2", ChannelMemberStatus.PENDING),
            ),
            version = 4,
            status = ChatChannelStatus.JOINED,
            createdAt = Instant.parse("2026-10-08T01:02:03Z"),
            updatedAt = Instant.parse("2026-10-08T01:02:04.123Z"),
        )

        assertEquals(
            Json.parseToJsonElement(
                """{
                    "id":"channel-1","name":"Team","ownerId":"owner-1",
                    "members":[{"peerId":"owner-1","status":"JOINED"},{"peerId":"peer-2","status":"PENDING"}],
                    "version":4,"status":"JOINED",
                    "createdAt":"2026-10-08T01:02:03Z","updatedAt":"2026-10-08T01:02:04.123Z"
                }""",
            ),
            channelFacts(channel),
        )
    }
}
