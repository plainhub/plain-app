package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.features.feed.FeedFontScale
import com.ismartcoding.plain.preferences.FeedFontScalePreference
import kotlinx.coroutines.launch

class FeedEntryViewModel : ViewModel() {
    val showSelectTagsDialog = mutableStateOf(false)
    val item = mutableStateOf<DFeedEntry?>(null)
    val feed = mutableStateOf<DFeed?>(null)
    val content = mutableStateOf("")
    val fetchingContent = mutableStateOf(false)

    // Article text scale: index into FeedFontScale.values, persisted.
    val fontScaleIndex = mutableIntStateOf(FeedFontScalePreference.default)

    init {
        viewModelScope.launch {
            fontScaleIndex.intValue = FeedFontScalePreference.getAsync()
        }
    }

    fun setFontScaleIndex(index: Int) {
        fontScaleIndex.intValue = index
        viewModelScope.launch {
            FeedFontScalePreference.putAsync(index)
        }
    }
}
