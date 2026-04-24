package com.ismartcoding.plain.ui.page.docs

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.extensions.isGestureInteractionMode
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.preferences.DocSortByPreference
import com.ismartcoding.plain.preferences.DocTabsModePreference
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DocsPageEffects(
    docsState: DocsPageState,
    docsVM: DocsViewModel,
    tagsVM: TagsViewModel,
    mediaFoldersVM: MediaFoldersViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedFlow = Channel.sharedFlow

    LaunchedEffect(Unit) {
        docsVM.hasPermission.value = AppFeatureType.FILES.hasPermission(context)
        if (docsVM.hasPermission.value) {
            scope.launch(Dispatchers.IO) {
                docsVM.tabsShowTags.value = DocTabsModePreference.getAsync(context)
                docsVM.sortBy.value = DocSortByPreference.getValueAsync(context)
                tagsVM.loadAsync()
                mediaFoldersVM.loadAsync(context)
                docsVM.loadAsync(context, tagsVM)
            }
        }
    }

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            if (event is PermissionsResultEvent) {
                docsVM.hasPermission.value = AppFeatureType.FILES.hasPermission(context)
                scope.launch(Dispatchers.IO) {
                    docsVM.sortBy.value = DocSortByPreference.getValueAsync(context)
                    docsVM.loadAsync(context, tagsVM)
                }
            }
        }
    }

    LaunchedEffect(docsState.dragSelectState.selectMode, !context.isGestureInteractionMode()) {
        if (docsState.dragSelectState.selectMode || !context.isGestureInteractionMode()) {
            docsState.scrollBehavior.reset()
        }
    }
}
