package com.ismartcoding.plain

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks `apitest/DATABASE.sql` against the Room schema JSON: the committed DDL
 * must equal the freshly generated output byte for byte, so any DB change
 * without regenerating the file fails this test.
 *
 * After an intentional change: run
 * `./gradlew :shared:testAndroidHostTest --tests "com.ismartcoding.plain.DbSchemaPrintTest"`
 * to regenerate, then commit the file together with the change.
 */
class DbSchemaTest {
    @Test
    fun databaseSqlIsUpToDate() {
        assertEquals(
            DbSchemaSql.build(),
            File("apitest/DATABASE.sql").readText(),
            "apitest/DATABASE.sql is stale. Regenerate via DbSchemaPrintTest and commit.",
        )
    }
}
