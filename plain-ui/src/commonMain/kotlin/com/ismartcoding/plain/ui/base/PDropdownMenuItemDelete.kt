package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.delete as ui_string_delete
import com.ismartcoding.plain.ui.resources.trash_2 as ui_drawable_trash_2
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PDropdownMenuItemDelete(modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    PDropdownMenuItem(
        text = { Text(stringResource(UiRes.string.ui_string_delete), color = MaterialTheme.colorScheme.error) },
        leadingIcon = {
            Icon(
                painter = painterResource(UiRes.drawable.ui_drawable_trash_2),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        },
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}
