package com.ismartcoding.plain.chat.peer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GraphQLResponseTest {

    // ── isSuccess ─────────────────────────────────────────────────

    @Test
    fun `isSuccess is true when no errors and no exception`() {
        val response = GraphQLResponse(data = "{}", errors = null, exception = null)
        assertTrue(response.isSuccess)
    }

    @Test
    fun `isSuccess is true when errors is empty list and no exception`() {
        val response = GraphQLResponse(data = "{}", errors = emptyList(), exception = null)
        assertTrue(response.isSuccess)
    }

    @Test
    fun `isSuccess is false when errors list is non-empty`() {
        val response = GraphQLResponse(
            data = null,
            errors = listOf(GraphQLError("Unauthorized")),
            exception = null,
        )
        assertFalse(response.isSuccess)
    }

    @Test
    fun `isSuccess is false when exception is set even if errors is null`() {
        // Reproduces the 401 scenario: HTTP failure produces an exception with no parsed errors.
        val response = GraphQLResponse(
            data = null,
            errors = null,
            exception = Exception("401 - Unauthorized"),
        )
        assertFalse(response.isSuccess)
    }

    @Test
    fun `isSuccess is false when exception is set even if errors is empty`() {
        val response = GraphQLResponse(
            data = null,
            errors = emptyList(),
            exception = Exception("401 - Unauthorized"),
        )
        assertFalse(response.isSuccess)
    }

    @Test
    fun `isSuccess is false when both errors and exception are set`() {
        val response = GraphQLResponse(
            data = null,
            errors = listOf(GraphQLError("BadRequest")),
            exception = Exception("network"),
        )
        assertFalse(response.isSuccess)
    }

    // ── getError ──────────────────────────────────────────────────

    @Test
    fun `getError returns joined error messages when errors present`() {
        val response = GraphQLResponse(
            data = null,
            errors = listOf(GraphQLError("a"), GraphQLError("b")),
            exception = Exception("ignored"),
        )
        assertEquals("a, b", response.getError())
    }

    @Test
    fun `getError falls back to exception message when no errors`() {
        val response = GraphQLResponse(
            data = null,
            errors = null,
            exception = Exception("401 - Unauthorized"),
        )
        assertEquals("401 - Unauthorized", response.getError())
    }

    @Test
    fun `getError falls back to exception message when errors is empty`() {
        val response = GraphQLResponse(
            data = null,
            errors = emptyList(),
            exception = Exception("401 - Unauthorized"),
        )
        assertEquals("401 - Unauthorized", response.getError())
    }

    @Test
    fun `getError returns Unknown error when no errors and no exception`() {
        val response = GraphQLResponse(data = null, errors = null, exception = null)
        assertEquals("Unknown error", response.getError())
    }

    @Test
    fun `getError returns Unknown error when errors is empty and no exception`() {
        val response = GraphQLResponse(data = null, errors = emptyList(), exception = null)
        assertEquals("Unknown error", response.getError())
    }

    // ── default args ──────────────────────────────────────────────

    @Test
    fun `default-constructed response has null fields and is considered successful`() {
        val response = GraphQLResponse()
        assertNull(response.data)
        assertNull(response.errors)
        assertNull(response.exception)
        assertTrue(response.isSuccess)
    }
}
