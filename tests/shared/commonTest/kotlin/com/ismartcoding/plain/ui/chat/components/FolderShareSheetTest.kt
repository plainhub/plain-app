package com.ismartcoding.plain.ui.chat.components

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FolderShareSheetTest {
    @Test
    fun listingFailureStaysInTheShareSheetInsteadOfEscapingToTheApplication() = runTest {
        val denied = IllegalStateException("Directory unavailable")
        val result = loadFolderShareEntries { throw denied }
        assertSame(denied, result.exceptionOrNull())
        assertTrue(loadFolderShareEntries { emptyList() }.isSuccess)
    }

    @Test
    fun dismissedShareSheetStillCancelsItsPendingListing() = runTest {
        assertFailsWith<CancellationException> {
            loadFolderShareEntries { throw CancellationException("Share sheet closed") }
        }
    }
}
