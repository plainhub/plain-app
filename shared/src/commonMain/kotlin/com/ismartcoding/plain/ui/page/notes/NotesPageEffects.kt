package com.ismartcoding.plain.ui.page.notes

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.platform.IODispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotesPageEffects(
    notesVM: NotesViewModel, tagsVM: TagsViewModel,
    scrollBehavior: TopAppBarScrollBehavior,
    scrollState: LazyListState,
    scope: CoroutineScope, isFirstTime: MutableState<Boolean>,
) {
    LaunchedEffect(Unit) {
        scope.launch(IODispatcher) { notesVM.loadAsync(tagsVM) }
    }

    LaunchedEffect(notesVM.selectMode.value) {
        if (notesVM.selectMode.value) scrollBehavior.reset()
    }

    LaunchedEffect(notesVM.trash.value, notesVM.tag.value) {
        if (isFirstTime.value) { isFirstTime.value = false; return@LaunchedEffect }
        scope.launch { scrollBehavior.reset(); scrollState.scrollToItem(0) }
        scope.launch(IODispatcher) { notesVM.loadAsync(tagsVM) }
    }
}
