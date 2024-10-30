package com.ismartcoding.plain.extensions

import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileSortBy

fun List<DFile>.sorted(sortBy: FileSortBy): List<DFile> {
    val comparator = compareBy<DFile> { if (it.isDir) 0 else 1 }
    return when (sortBy) {
        FileSortBy.NAME_ASC -> {
            this.sortedWith(comparator.thenBy { it.name.lowercase() })
        }

        FileSortBy.NAME_DESC -> {
            this.sortedWith(comparator.thenByDescending { it.name.lowercase() })
        }

        FileSortBy.SIZE_ASC -> {
            this.sortedWith(comparator.thenBy { it.size })
        }

        FileSortBy.SIZE_DESC -> {
            this.sortedWith(comparator.thenByDescending { it.size })
        }

        FileSortBy.DATE_ASC -> {
            this.sortedWith(comparator.thenBy { it.updatedAt })
        }

        FileSortBy.DATE_DESC -> {
            this.sortedWith(comparator.thenByDescending { it.updatedAt })
        }
    }
}