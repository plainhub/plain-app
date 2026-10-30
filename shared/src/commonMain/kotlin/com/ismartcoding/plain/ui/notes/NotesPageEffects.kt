package com.ismartcoding.plain.ui.notes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import com.ismartcoding.plain.ui.media.extensions.reset
import com.ismartcoding.plain.ui.notes.NotesViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotesPageEffects(
    tagsVM: TagsViewModel,
    scrollBehavior: TopAppBarScrollBehavior,
    scrollState: LazyListState,
    scope: CoroutineScope, isFirstTime: MutableState<Boolean>,
    notesVM: NotesViewModel = NotesViewModel.local,
) {
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) { notesVM.loadAsync(tagsVM) }
    }

    LaunchedEffect(notesVM.selectMode.value) {
        if (notesVM.selectMode.value) scrollBehavior.reset()
    }

    LaunchedEffect(notesVM.trash.value, notesVM.tag.value) {
        if (isFirstTime.value) { isFirstTime.value = false; return@LaunchedEffect }
        scope.launch { scrollBehavior.reset(); scrollState.scrollToItem(0) }
        scope.launch(Dispatchers.IO) { notesVM.loadAsync(tagsVM) }
    }
}
