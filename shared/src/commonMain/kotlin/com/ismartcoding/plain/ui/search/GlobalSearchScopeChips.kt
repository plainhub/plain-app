package com.ismartcoding.plain.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PIcon
import com.ismartcoding.plain.ui.search.GlobalSearchDomain
import org.jetbrains.compose.resources.stringResource

/** Sticky scope selector: All + every domain when idle, All + hit domains while searching. */
@Composable
fun GlobalSearchScopeChips(
    selected: GlobalSearchDomain?,
    chips: List<GlobalSearchDomain?>,
    onSelect: (GlobalSearchDomain?) -> Unit,
    deviceFilter: (@Composable () -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        deviceFilter?.invoke()
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())
                .padding(start = if (deviceFilter == null) 16.dp else 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            chips.forEach { chip ->
                PFilterChip(
                    selected = selected == chip,
                    onClick = { onSelect(chip) },
                    label = {
                        if (chip == null) {
                            Text(stringResource(Res.string.all))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PIcon(icon = chip.iconRes, contentDescription = null, modifier = Modifier.size(16.dp))
                                HorizontalSpace(8.dp)
                                Text(stringResource(chip.labelRes))
                            }
                        }
                    },
                )
            }
        }
    }
}
