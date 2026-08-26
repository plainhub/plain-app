package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.theme.red
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Sidebar drawer item for a tag. Long-press opens a menu to edit or delete the tag.
 */
@Composable
fun MediaSidebarTagItem(
    tag: DTag,
    isSelected: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        SidebarItem(
            label = tag.name,
            icon = Res.drawable.tag,
            isSelected = isSelected,
            badge = tag.count.toString(),
            onClick = onClick,
            onLongClick = { menuExpanded = true },
        )

        if (menuExpanded) {
            PDropdownMenu(
                expanded = true,
                onDismissRequest = { menuExpanded = false }
            ) {
                PDropdownMenuItem(
                    text = { Text(stringResource(Res.string.edit)) },
                    leadingIcon = { Icon(painterResource(Res.drawable.square_pen), null, modifier = Modifier.size(20.dp)) },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    }
                )
                PDropdownMenuItem(
                    text = { Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.red) },
                    leadingIcon = { Icon(painterResource(Res.drawable.trash_2), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.red) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}
