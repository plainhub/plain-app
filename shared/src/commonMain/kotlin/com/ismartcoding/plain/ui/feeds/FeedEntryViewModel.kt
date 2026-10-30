package com.ismartcoding.plain.ui.feeds

import com.ismartcoding.plain.rust.graphql.ContentSource
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.models.Feed
import com.ismartcoding.plain.models.FeedEntry
import com.ismartcoding.plain.preferences.*
import kotlinx.coroutines.launch

class FeedEntryViewModel internal constructor(val source: ContentSource = ContentSource.Local) : ViewModel() {
    val showSelectTagsDialog = mutableStateOf(false)
    val item = mutableStateOf<FeedEntry?>(null)
    val feed = mutableStateOf<Feed?>(null)
    val content = mutableStateOf("")
    val fetchingContent = mutableStateOf(false)

    // Article text scale: index into FeedFontScale.values, persisted.
    val fontScaleIndex = mutableIntStateOf(Prefs.feedFontScale.value)

    init {
        viewModelScope.launch {
            fontScaleIndex.intValue = Prefs.feedFontScale.value
        }
    }

    fun setFontScaleIndex(index: Int) {
        fontScaleIndex.intValue = index
        viewModelScope.launch {
            Prefs.set(PrefKeys.FEED_FONT_SCALE, index)
        }
    }
}
