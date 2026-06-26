package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DFeedEntry

class FeedEntryViewModel : ViewModel() {
    val showSelectTagsDialog = mutableStateOf(false)
    val item = mutableStateOf<DFeedEntry?>(null)
    val feed = mutableStateOf<DFeed?>(null)
    val content = mutableStateOf("")
    val fetchingContent = mutableStateOf(false)
}
