package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.MutableState
import com.ismartcoding.plain.data.IData

interface ISearchableViewModel<T : IData> {
    val showSearchBar: MutableState<Boolean>
    val searchActive: MutableState<Boolean>
    val queryText: MutableState<String>
}

fun <T : IData> ISearchableViewModel<T>.enterSearchMode() {
    showSearchBar.value = true
    searchActive.value = true
}

fun <T : IData> ISearchableViewModel<T>.exitSearchMode() {
    showSearchBar.value = false
    searchActive.value = false
    queryText.value = ""
}