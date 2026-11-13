package com.ismartcoding.plain.features.shares

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.ShareInput
import com.ismartcoding.plain.models.SharePath
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SharesApiTest {
    @Test
    fun create_and_update_send_one_complete_typed_input() = runTest {
        val input = ShareInput("Share", "", null, true, listOf(SharePath("/alias", "/owned/folder", true)))
        var calls = 0
        val client = SharesApi(GraphqlClient { operation ->
            calls++
            assertEquals(JsonHelper.jsonEncodeToElement(input), operation.variables["input"])
            val field = if (calls == 1) "createShare" else "updateShare"
            GraphqlResponse(data = JsonHelper.jsonDecode("""{"$field":{"id":"share","name":"Share","password":"","expiresAt":null,"readOnly":true,"data":[{"virtualPath":"/alias","realPath":"/owned/folder","isDir":true}],"url":"https://device.test/s/share#key"}}"""))
        })
        val created = client.createShare(input)
        val updated = client.updateShare(created.id, input)
        assertEquals(input.data, updated.data)
        assertEquals("https://device.test/s/share#key", updated.url)
        assertEquals(2, calls)
    }
    @Test
    fun management_errors_do_not_use_another_device() = runTest {
        var calls = 0
        val client = SharesApi(GraphqlClient { calls++; error("target unavailable") })
        assertEquals("target unavailable", assertFailsWith<IllegalStateException> { client.listShares() }.message)
        assertEquals(1,calls)
    }
}
