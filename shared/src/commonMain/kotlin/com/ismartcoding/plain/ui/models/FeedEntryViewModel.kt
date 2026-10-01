package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.preferences.*

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.features.feed.FeedFontScale
import kotlinx.coroutines.launch

class FeedEntryViewModel : ViewModel() {
    val showSelectTagsDialog = mutableStateOf(false)
    val item = mutableStateOf<DFeedEntry?>(null)
    val feed = mutableStateOf<DFeed?>(null)
    val content = mutableStateOf("")
    val fetchingContent = mutableStateOf(false)

    // Article text scale: index into FeedFontScale.values, persisted.
    val fontScaleIndex = mutableIntStateOf(UserPrefs.feedFontScale.default)

    init {
        viewModelScope.launch {
            fontScaleIndex.intValue = UserPrefs.feedFontScale.value
        }
    }

    fun setFontScaleIndex(index: Int) {
        fontScaleIndex.intValue = index
        viewModelScope.launch {
            UserPrefs.feedFontScale.value = index
        }
    }
}
