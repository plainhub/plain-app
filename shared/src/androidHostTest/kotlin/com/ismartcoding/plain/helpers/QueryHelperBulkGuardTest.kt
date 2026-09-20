package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class QueryHelperBulkGuardTest {

    @Test
    fun blankQueriesAreRejected() {
        listOf("", "  ", "\t", " \n ").forEach { q ->
            val error = assertFailsWith<GraphQLError> { QueryHelper.requireExplicitBulkQuery(q) }
            assertTrue(
                error.message!!.contains("all:true"),
                "Guard error must point at the all:true sentinel, got: ${error.message}",
            )
        }
    }

    @Test
    fun explicitTargetsPass() {
        // Whole-table sentinel, targeted ids, field filters — all valid bulk targets.
        listOf("all:true", "ids:1,2", "trash:true", "text:foo", "bucket_id:12").forEach { q ->
            QueryHelper.requireExplicitBulkQuery(q)
        }
    }
}
