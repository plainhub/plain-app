package com.ismartcoding.plain.tests

import android.content.ContentUris
import android.content.ContentValues
import android.provider.Telephony
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertNull
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.platform.media.deleteSmsRows
import com.ismartcoding.plain.platform.system.ShizukuHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class ShizukuSmsDeletionTest {
    @Test fun deletesTheSyntheticInboxRow() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("shizukuSmsTest") == "true")
        assertTrue(ShizukuHelper.isGranted())
        val test = InstrumentationRegistry.getInstrumentation()
        val resolver = test.targetContext.contentResolver
        val automation = test.uiAutomation
        automation.adoptShellPermissionIdentity()
        val uri = try {
            requireNotNull(resolver.insert(Telephony.Sms.Inbox.CONTENT_URI, ContentValues().apply {
                put(Telephony.Sms.ADDRESS, "5550100")
                put(Telephony.Sms.BODY, "Synthetic Shizuku deletion fixture")
            }))
        } finally { automation.dropShellPermissionIdentity() }
        val id = ContentUris.parseId(uri).toString()
        try {
            val result = deleteSmsRows(listOf(id))
            assertEquals(listOf(id), result.deletedIds)
            assertTrue(result.failedIds.isEmpty())
        } finally {
            automation.adoptShellPermissionIdentity()
            try { resolver.delete(uri, null, null) } finally { automation.dropShellPermissionIdentity() }
        }
    }

    @Test fun deletesSelectedTestSmsThroughGraphql() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.containsKey("fixtureMarker"))
        val id = args.getString("fixtureId")!!.toLong()
        val marker = args.getString("fixtureMarker")!!
        require(marker.matches(Regex("PlainAppFixture[0-9a-f]{32}")))
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val uri = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, id)
        fun body() = resolver.query(uri, arrayOf("body"), null, null, null)!!.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
        assertEquals(marker, body())
        val result = GraphqlApi.execute(GraphqlRequest<JsonObject>("""mutation { deleteSms(query:"ids:$id") { affectedCount } }""", field = "deleteSms"))
        assertEquals("1", result["affectedCount"].toString())
        assertNull(body())
    }
}
