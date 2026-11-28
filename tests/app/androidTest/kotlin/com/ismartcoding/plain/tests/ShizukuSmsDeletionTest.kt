package com.ismartcoding.plain.tests

import android.content.ContentUris
import android.content.ContentValues
import android.provider.Telephony
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
}
