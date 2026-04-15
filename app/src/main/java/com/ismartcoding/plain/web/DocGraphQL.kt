package com.ismartcoding.plain.web

import android.provider.MediaStore
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import com.apurebase.kgraphql.schema.execution.Executor
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.enums.FileType
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.media.FileMediaStoreHelper
import com.ismartcoding.plain.web.models.DocExtGroup
import com.ismartcoding.plain.web.models.toDocModel

private fun parseDocFileSize(s: String): Long? {
    val m = Regex("^([\\d.]+)\\s*(b|kb|mb|gb|tb)?$", RegexOption.IGNORE_CASE).matchEntire(s.trim()) ?: return null
    val num = m.groupValues[1].toDoubleOrNull() ?: return null
    return when (m.groupValues[2].lowercase()) {
        "kb" -> (num * 1024).toLong()
        "mb" -> (num * 1024 * 1024).toLong()
        "gb" -> (num * 1024 * 1024 * 1024).toLong()
        "tb" -> (num * 1024L * 1024 * 1024 * 1024).toLong()
        else -> num.toLong()
    }
}

private fun matchDocFileSize(fileSize: Long, op: String, filterSize: Long): Boolean {
    return when (op) {
        ">" -> fileSize > filterSize
        ">=" -> fileSize >= filterSize
        "<" -> fileSize < filterSize
        "<=" -> fileSize <= filterSize
        "!=" -> fileSize != filterSize
        else -> fileSize == filterSize
    }
}

fun SchemaBuilder.addDocQueries() {
    query("docs") {
        configure {
            executor = Executor.DataLoaderPrepared
        }
        resolver { offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
            val context = MainApp.instance
            Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
            val fields = SearchHelper.parse(query)
            val extField = fields.find { it.name == "ext" }
            val textField = fields.find { it.name == "text" }
            val fileSizeField = fields.find { it.name == "file_size" }
            FileMediaStoreHelper.getAllByFileTypeAsync(context, MediaStore.VOLUME_EXTERNAL_PRIMARY, FileType.DOCUMENT, sortBy)
                .filter { file ->
                    val extMatch = extField == null || file.name.substringAfterLast('.', "").lowercase() == extField.value.lowercase()
                    val textMatch = textField == null || file.name.contains(textField.value, ignoreCase = true)
                    val sizeMatch = if (fileSizeField == null) true else {
                        val bytes = parseDocFileSize(fileSizeField.value) ?: return@filter false
                        matchDocFileSize(file.size, fileSizeField.op, bytes)
                    }
                    extMatch && textMatch && sizeMatch
                }
                .drop(offset).take(limit).map { it.toDocModel() }
        }
    }

    query("docCount") {
        resolver { query: String ->
            if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndCanAsync(MainApp.instance)) {
                val fields = SearchHelper.parse(query)
                val extField = fields.find { it.name == "ext" }
                val textField = fields.find { it.name == "text" }
                val fileSizeField = fields.find { it.name == "file_size" }
                FileMediaStoreHelper.getAllByFileTypeAsync(
                    MainApp.instance, MediaStore.VOLUME_EXTERNAL_PRIMARY, FileType.DOCUMENT, FileSortBy.DATE_DESC
                ).filter { file ->
                    val extMatch = extField == null || file.name.substringAfterLast('.', "").lowercase() == extField.value.lowercase()
                    val textMatch = textField == null || file.name.contains(textField.value, ignoreCase = true)
                    val sizeMatch = if (fileSizeField == null) true else {
                        val bytes = parseDocFileSize(fileSizeField.value) ?: return@filter false
                        matchDocFileSize(file.size, fileSizeField.op, bytes)
                    }
                    extMatch && textMatch && sizeMatch
                }.size
            } else {
                0
            }
        }
    }

    query("docExtGroups") {
        resolver { ->
            if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndCanAsync(MainApp.instance)) {
                FileMediaStoreHelper.getAllByFileTypeAsync(
                    MainApp.instance, MediaStore.VOLUME_EXTERNAL_PRIMARY, FileType.DOCUMENT, FileSortBy.NAME_ASC
                )
                    .groupBy { it.name.substringAfterLast('.', "").lowercase() }
                    .map { DocExtGroup(it.key.uppercase(), it.value.size) }
                    .sortedBy { it.ext }
            } else {
                emptyList()
            }
        }
    }
}
