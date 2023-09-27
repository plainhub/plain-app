package com.ismartcoding.plain.features.vocabulary

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DVocabulary
import kotlinx.datetime.Clock

object VocabularyList {
    fun getItemsAsync(): List<DVocabulary> {
        return AppDatabase.instance.vocabularyDao().getAll(TempData.selectedBoxId)
    }

    fun addOrUpdateAsync(
        id: String,
        updateItem: DVocabulary.() -> Unit,
    ) {
        var item = if (id.isNotEmpty()) AppDatabase.instance.vocabularyDao().getById(id) else null
        var isInsert = false
        if (item == null) {
            item = DVocabulary()
            isInsert = true
        }

        item.updatedAt = Clock.System.now()

        updateItem(item)

        if (isInsert) {
            AppDatabase.instance.vocabularyDao().insert(item)
        } else {
            AppDatabase.instance.vocabularyDao().update(item)
        }
    }

    fun deleteAsync(item: DVocabulary) {
        return AppDatabase.instance.vocabularyDao().delete(item)
    }
}
