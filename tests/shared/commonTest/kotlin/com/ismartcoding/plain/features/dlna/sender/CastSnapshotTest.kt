package com.ismartcoding.plain.features.dlna.sender

import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CastSnapshotTest {
    @Test
    fun stateQueryDecodesTheSameSnapshotAsSenderCommands() = runTest {
        var requests = 0
        val client = GraphqlClient { request ->
            requests++
            assertEquals("query", request.query.trim().substringBefore(' '))
            JsonHelper.jsonDecode<GraphqlResponse>("""{"data":{"dlnaSenderState":{"version":7,"devices":[],"currentDevice":null,"items":[{"path":"/target/track.mp3","title":"Track","albumArt":"","artist":"","durationMs":1000,"audio":true}],"currentUri":"","playing":false,"progressMs":0,"durationMs":1000,"supportsCallback":false}}}""")
        }
        val snapshot = client.execute(Query.dlnaSenderState())
        assertEquals(7L, snapshot.version)
        assertEquals("/target/track.mp3", snapshot.items.single().path)
        assertFalse(snapshot.playing)
        assertEquals(1, requests)
    }
    @Test
    fun sourceScopedSenderKeepsTargetPathsAndRejectsStaleSnapshotsWithoutChangingLocalProjection() = runTest {
        val localItems = RustDlnaSender.items.value
        val localUri = RustDlnaSender.currentUri.value
        val path = "/target/private/track.mp3"
        val client = DlnaSenderClient(GraphqlClient { request ->
            assertEquals(path, (request.variables["item"] as kotlinx.serialization.json.JsonObject)["path"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content })
            JsonHelper.jsonDecode<GraphqlResponse>("""{"data":{"castDlnaMedia":{"version":10,"items":[{"path":"$path","title":"Track"}],"currentUri":"$path","playing":true}}}""")
        })
        client.call(CastCommand.Cast(CastItem(path, "Track")))
        client.apply(CastSnapshot(version = 9, items = emptyList()))
        assertEquals(path, client.items.value.single().path)
        assertEquals(path, client.currentUri.value)
        assertEquals(localItems, RustDlnaSender.items.value)
        assertEquals(localUri, RustDlnaSender.currentUri.value)
    }
}
