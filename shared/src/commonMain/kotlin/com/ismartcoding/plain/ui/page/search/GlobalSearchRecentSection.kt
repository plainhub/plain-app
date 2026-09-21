package com.ismartcoding.plain.ui.page.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PInputChip
import com.ismartcoding.plain.ui.base.PTextButton
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Recent search terms as Material input chips. The trash icon enters edit
 * mode (WeChat-style): chips reveal an individual delete button and the
 * header swaps to Clear + Done. Clearing empties the section, which drops
 * the edit state with it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlobalSearchRecentSection(
    recent: List<String>,
    onSearch: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (recent.isEmpty()) return
    var editing by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.recent_searches),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (editing) {
            PTextButton(text = stringResource(Res.string.clear), onClick = onClear, buttonSize = ButtonSize.SMALL)
            HorizontalSpace(4.dp)
            PFilledButton(
                text = stringResource(Res.string.done),
                onClick = { editing = false },
                buttonSize = ButtonSize.SMALL,
            )
        } else {
            PIconButton(
                icon = Res.drawable.trash_2,
                contentDescription = stringResource(Res.string.clear),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                editing = true
            }
        }
    }
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        recent.forEach { term ->
            PInputChip(
                text = term,
                icon = painterResource(Res.drawable.history),
                closable = editing,
                closeContentDescription = stringResource(Res.string.delete),
                onClose = { onRemove(term) },
                onClick = { if (!editing) onSearch(term) },
            )
        }
    }
}
