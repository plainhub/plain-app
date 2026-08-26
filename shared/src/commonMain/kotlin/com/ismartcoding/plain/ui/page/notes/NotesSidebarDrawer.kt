package com.ismartcoding.plain.ui.page.notes

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.platform.IODispatcher
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.MediaSidebarTagItem
import com.ismartcoding.plain.ui.components.SidebarItem
import com.ismartcoding.plain.ui.components.SidebarSectionHeader
import com.ismartcoding.plain.ui.components.TagNameDialog
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Sidebar drawer content for the notes page. Shows All, Trash, and Tags.
 * Selecting a filter only updates [NotesViewModel.trash] / [NotesViewModel.tag];
 * the notes page reacts to the change and reloads the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesSidebarDrawer(
    notesVM: NotesViewModel,
    tagsVM: TagsViewModel,
    drawerState: DrawerState,
) {
    val tags by tagsVM.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var tagsExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch(IODispatcher) { tagsVM.loadAsync() }
    }

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(NavigationDrawerItemDefaults.ItemPadding)
    ) {
        // Header spacer
        VerticalSpace(dp = 16.dp)

        // All
        SidebarItem(
            label = stringResource(Res.string.all),
            icon = Res.drawable.layout_grid,
            isSelected = !notesVM.trash.value && notesVM.tag.value == null,
            onClick = {
                notesVM.trash.value = false
                notesVM.tag.value = null
                scope.launch { drawerState.close() }
            },
            badge = notesVM.total.intValue.toString()
        )

        // Trash
        SidebarItem(
            label = stringResource(Res.string.trash),
            icon = Res.drawable.trash_2,
            isSelected = notesVM.trash.value,
            onClick = {
                notesVM.trash.value = true
                notesVM.tag.value = null
                scope.launch { drawerState.close() }
            },
            badge = notesVM.totalTrash.intValue.toString()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Tags Section
        SidebarSectionHeader(
            title = stringResource(Res.string.tags),
            isExpanded = tagsExpanded,
            onToggle = { tagsExpanded = !tagsExpanded },
            onAction = { tagsVM.showAddDialog() },
            actionIcon = Res.drawable.plus
        )

        if (tagsExpanded) {
            tags.forEach { tag ->
                MediaSidebarTagItem(
                    tag = tag,
                    isSelected = !notesVM.trash.value && notesVM.tag.value?.id == tag.id,
                    onEdit = { tagsVM.showEditDialog(tag) },
                    onDelete = {
                        DialogHelper.confirmToDelete {
                            tagsVM.deleteTag(tag.id)
                        }
                    },
                    onClick = {
                        notesVM.trash.value = false
                        notesVM.tag.value = tag
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
        BottomSpace()
    }

    TagNameDialog(tagsVM) {
        tagsVM.loadAsync()
    }
}
