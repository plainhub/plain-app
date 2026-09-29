package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.ui.resources.file_text as ui_drawable_file_text
import com.ismartcoding.plain.ui.resources.folder as ui_drawable_folder
import org.jetbrains.compose.resources.painterResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.file_text as ui_drawable_file_text
import com.ismartcoding.plain.ui.resources.folder as ui_drawable_folder
import com.ismartcoding.plain.i18n.folder

/**
 * One row of a device-storage directory browser (folder pickers, share-item
 * pickers): folder/file icon + name + optional subtitle, optional trailing
 * checkbox. The ".." parent row passes [isDir] with a subtitle.
 */
@Composable
fun DirBrowserRow(
    name: String,
    subtitle: String = "",
    isDir: Boolean = true,
    selected: Boolean = false,
    showCheckbox: Boolean = false,
    onClick: () -> Unit,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(if (isDir) UiRes.drawable.ui_drawable_folder else UiRes.drawable.ui_drawable_file_text),
            contentDescription = name,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (showCheckbox) {
            CheckCircle(selected = selected, onClick = onCheckedChange?.let { onChecked -> { onChecked(!selected) } })
        }
    }
}
