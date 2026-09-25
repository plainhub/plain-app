package com.ismartcoding.plain

import java.io.File
import kotlin.test.Test

/**
 * Regenerates `apitest/DATABASE.sql` from the Room schema JSON (mirrors
 * PrintSchemaTest, which regenerates the SDL). Output is byte-stable when the
 * sources are unchanged; `DbSchemaTest` fails when the committed file goes
 * stale. Run to refresh:
 *
 * `./gradlew :shared:testAndroidHostTest --tests "com.ismartcoding.plain.DbSchemaPrintTest"`
 */
class DbSchemaPrintTest {
    @Test
    fun printSql() {
        File("apitest/DATABASE.sql").writeText(DbSchemaSql.build())
    }
}
