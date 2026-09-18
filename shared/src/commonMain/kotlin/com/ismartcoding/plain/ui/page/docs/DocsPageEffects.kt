package com.ismartcoding.plain.ui.page.docs

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.platform.isGestureInteractionMode
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.hasPermission
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.preferences.DocSortByPreference
import com.ismartcoding.plain.preferences.DocTabsModePreference
import com.ismartcoding.plain.ui.base.StoragePermissionResumeEffect
import com.ismartcoding.plain.ui.base.refreshStoragePermission
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.nav.NavLoadGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DocsPageEffects(
    docsState: DocsPageState,
    docsVM: DocsViewModel,
    tagsVM: TagsViewModel,
    mediaFoldersVM: MediaFoldersViewModel,
    initialLoadGate: NavLoadGate? = null,
) {
    val scope = rememberCoroutineScope()
    val sharedFlow = Channel.sharedFlow
    val reloadAfterGrant: () -> Unit = {
        scope.launch(Dispatchers.Default) {
            docsVM.tabsShowTags.value = DocTabsModePreference.getAsync()
            docsVM.sortBy.value = DocSortByPreference.getValueAsync()
            tagsVM.loadAsync()
            mediaFoldersVM.loadAsync()
            docsVM.loadAsync(tagsVM)
        }
    }

    LaunchedEffect(Unit) {
        docsVM.hasPermission.value = AppFeatureType.FILES.hasPermission()
        if (docsVM.hasPermission.value) {
            initialLoadGate?.await()
            reloadAfterGrant()
        }
    }

    StoragePermissionResumeEffect(docsVM.hasPermission, reloadAfterGrant)

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            if (event is PermissionsResultEvent) {
                refreshStoragePermission(docsVM.hasPermission, reloadAfterGrant)
            }
        }
    }

    LaunchedEffect(docsState.dragSelectState.selectMode, !isGestureInteractionMode()) {
        if (docsState.dragSelectState.selectMode || !isGestureInteractionMode()) {
            docsState.scrollBehavior.reset()
        }
    }
}
