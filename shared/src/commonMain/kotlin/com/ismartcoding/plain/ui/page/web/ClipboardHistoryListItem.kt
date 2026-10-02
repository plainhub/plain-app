package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.db.DClipboard
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.ui.base.PListItem
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.delete_forever as ui_drawable_delete_forever

@Composable
fun ClipboardHistoryListItem(entry: DClipboard, sourceName: String, onCopy: () -> Unit, onDelete: () -> Unit) {
    PListItem(
        modifier = Modifier.clickable { onCopy() },
        title = if (entry.sensitive) "••••••••" else entry.text.let { if (it.length > 200) it.take(200) + "…" else it },
        subtitle = buildString {
            append(entry.createdAt.formatDateTime())
            append(" · ")
            append(sourceName.ifBlank { stringResource(Res.string.clipboard_source_local) })
        },
        action = {
            Icon(
                painter = painterResource(UiRes.drawable.ui_drawable_delete_forever),
                contentDescription = stringResource(Res.string.delete),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable { onDelete() }.padding(8.dp),
            )
        },
    )
}
