package com.ismartcoding.plain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Builds the plain SQLite DDL (`apitest/DATABASE.sql`) from the Room schema
 * JSON (`room-db/schemas/.../<N>.json`, highest version): every entity's
 * `createSql` plus its index `createSql`, with `${TABLE_NAME}` substituted.
 *
 * The file is generated, never hand-edited: `DbSchemaTest` fails when it goes
 * stale; run [DbSchemaPrintTest] to regenerate. Paths are relative to the test
 * working dir (the `shared/` module dir), mirroring PrintSchemaTest.
 */
object DbSchemaSql {
    fun build(): String {
        val dir = File("../room-db/schemas/com.ismartcoding.plain.platform.AppDatabase")
        val file =
            dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                ?.maxBy { it.nameWithoutExtension.toInt() }
                ?: error("Room schema dir not found: ${dir.absolutePath}")
        val root = Json.parseToJsonElement(file.readText()).jsonObject
        val db = root["database"]!!.jsonObject
        val version = db["version"]!!.jsonPrimitive.int
        val entities =
            db["entities"]!!.jsonArray.map { it.jsonObject }.sortedBy { it["tableName"]!!.jsonPrimitive.content }

        val sb = StringBuilder()
        sb.appendLine("-- plain-app Room 数据库 DDL（Room version $version，${entities.size} 张表）")
        sb.appendLine("-- 自动生成，禁止手改。源：room-db/schemas/com.ismartcoding.plain.platform.AppDatabase/${file.name}")
        sb.appendLine("-- 再生：./gradlew :shared:testAndroidHostTest --tests \"com.ismartcoding.plain.DbSchemaPrintTest\"")
        sb.appendLine("-- 过期锁：DbSchemaTest")
        for (e in entities) {
            val table = e["tableName"]!!.jsonPrimitive.content
            sb.appendLine()
            sb.appendLine(e["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table) + ";")
            val indices = e["indices"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
            for (i in indices.sortedBy { it["name"]!!.jsonPrimitive.content }) {
                sb.appendLine(i["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table) + ";")
            }
        }
        return sb.toString()
    }
}
