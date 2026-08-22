package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.theme.red
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Sidebar drawer item for a tag. Long-press opens a menu to edit or delete the tag.
 */
@OptIn(ExperimentalFoundationApi::class)
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
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                ),
            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(Res.drawable.tag),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                HorizontalSpace(12.dp)
                Text(
                    text = tag.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = tag.count.toString(),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

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
