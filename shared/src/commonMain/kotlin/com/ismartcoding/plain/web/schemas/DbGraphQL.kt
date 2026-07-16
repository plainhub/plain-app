package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.platform.createDbTableRow
import com.ismartcoding.plain.platform.deleteDbTableRows
import com.ismartcoding.plain.platform.getDbPath
import com.ismartcoding.plain.platform.getDbTableInfo
import com.ismartcoding.plain.platform.getDbTableNames
import com.ismartcoding.plain.platform.getDbTableRowCount
import com.ismartcoding.plain.platform.getDbTableRows

fun SchemaBuilder.addDbSchema() {
    query("dbPath") {
        resolver { ->
            getDbPath()
        }
    }

    query("dbTables") {
        resolver { -> getDbTableNames() }
    }

    query("dbTableRowCount") {
        resolver("table") { table: String -> getDbTableRowCount(table) }
    }

    query("dbTableRows") {
        resolver("table", "offset", "limit") { table: String, offset: Int, limit: Int ->
            getDbTableRows(table, offset, limit)
        }
    }

    query("dbTableInfo") {
        resolver("table") { table: String -> getDbTableInfo(table) }
    }

    mutation("createDbTableRow") {
        resolver("table", "row") { table: String, row: String ->
            createDbTableRow(table, row)
        }
    }

    mutation("deleteDbTableRows") {
        resolver("table", "ids") { table: String, ids: List<String> ->
            deleteDbTableRows(table, ids)
        }
    }
}
